// ===========================
// Chat Page Logic (js/chat.js)
// ===========================

// ---- Config ----
const DEFAULT_API_BASE = window.location.origin;//"http://localhost:8082";
const API_BASE = (window.API_BASE || DEFAULT_API_BASE).replace(/\/+$/, "");
// Primary chat endpoint (expects either streaming text/event-stream or a JSON { reply: "..."} )
// Adjust if your backend differs:
const CHAT_ENDPOINT = "/api/chat/ask";

// If your API uses cookies/sessions, flip to true
const USE_CREDENTIALS = false;

// ---- DOM ----
const $msgs     = () => document.getElementById("chatMessages");
const $input    = () => document.getElementById("chatInput");
const $send     = () => document.getElementById("sendBtn");
const $stop     = () => document.getElementById("stopBtn");
const $typing   = () => document.getElementById("typing");
const $clear    = () => document.getElementById("clearBtn");

const $tplUser  = () => document.getElementById("tplUserMsg");
const $tplAsst  = () => document.getElementById("tplAssistantMsg");

const $toast    = () => document.getElementById("toast");

// ---- Conversation state (kept in-memory) ----
let messages = [];   // { role: "user"|"assistant"|"system", content: "..." }
let abortController = null;

// ---- UI helpers ----
function toast(msg) {
    const box = document.createElement("div");
    box.textContent = msg;
    box.style.cssText = "background:#111827;color:#fff;padding:10px 12px;border-radius:10px;box-shadow:0 8px 24px rgba(0,0,0,.18)";
    $toast().appendChild(box);
    setTimeout(() => box.remove(), 3000);
}

function cloneTpl(tpl) {
    const node = tpl.content.firstElementChild.cloneNode(true);
    return node;
}

function appendUserMessage(text) {
    const node = cloneTpl($tplUser());
    node.querySelector(".content").textContent = text;
    $msgs().appendChild(node);
    autoscroll();
}

function appendAssistantMessage(initialText = "") {
    const node = cloneTpl($tplAsst());
    const content = node.querySelector(".content");
    content.textContent = initialText;
    $msgs().appendChild(node);
    autoscroll();
    return content; // return the content node for streaming updates
}

function setTyping(on) {
    if (!$typing()) return;
    $typing().classList.toggle("hidden", !on);
}

function setSending(on) {
    if ($send()) $send().disabled = on;
    if ($stop()) $stop().disabled = !on;
    setTyping(on);
}

function autoscroll() {
    // scroll the message container to bottom smoothly
    const el = document.scrollingElement || document.documentElement;
    el.scrollTo({ top: el.scrollHeight, behavior: "smooth" });
}

function clearChat() {
    messages = [];
    // keep the initial welcome message (first .msg.assistant) if present
    const container = $msgs();
    if (!container) return;
    // remove all messages except the first assistant welcome (if you want to preserve it)
    const keepFirstAssistant = container.querySelector(".msg.assistant");
    container.innerHTML = "";
    if (keepFirstAssistant) container.appendChild(keepFirstAssistant);
}

// ---- Networking ----

// Generic JSON fetch (for fallbacks)
async function jsonFetch(url, opts = {}) {
    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), 30000); // 30s
    try {
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", ...(opts.headers || {}) },
            credentials: USE_CREDENTIALS ? "include" : "same-origin",
            mode: "cors",
            signal: controller.signal,
            body: JSON.stringify(opts.body || {})
        });
        clearTimeout(t);
        if (!res.ok) {
            const txt = await res.text().catch(() => "");
            throw new Error(`HTTP ${res.status}${txt ? `: ${txt}` : ""}`);
        }
        const ct = res.headers.get("content-type") || "";
        if (ct.includes("application/json")) {
            return await res.json();
        }
        // fallback: plain text
        const txt = await res.text();
        return { reply: txt };
    } catch (err) {
        clearTimeout(t);
        throw err;
    }
}

/**
 * Send chat to server.
 * Backend options (use whichever you implement):
 * 1) Streaming (text/event-stream or chunked text) -> this function streams into the UI
 * 2) Non-streaming JSON -> { reply: "..." }
 */
async function sendChatStreaming(payload, onToken) {
    abortController = new AbortController();
    const url = `${API_BASE}${CHAT_ENDPOINT}`;

    const res = await fetch(url, {
        method: "POST",
        headers: {
            "Accept": "text/event-stream, text/plain, application/json",
            "Content-Type": "application/json"
        },
        credentials: USE_CREDENTIALS ? "include" : "same-origin",
        mode: "cors",
        signal: abortController.signal,
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        const txt = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status}${txt ? `: ${txt}` : ""}`);
    }

    const ct = res.headers.get("content-type") || "";
    // If JSON, just parse once and return
    if (ct.includes("application/json")) {
        const data = await res.json();
        const text = data.reply || data.message || "";
        onToken(text); // single shot
        return;
    }

    // If readable stream (SSE/chunked)
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let partial = "";

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        partial += decoder.decode(value, { stream: true });

        // Handle Server-Sent Events: lines prefixed "data: ..."
        if (ct.includes("text/event-stream")) {
            const lines = partial.split("\n");
            // keep the last partial line in buffer
            partial = lines.pop() || "";
            for (const ln of lines) {
                const line = ln.trim();
                if (!line) continue;
                if (line.startsWith("data:")) {
                    const chunk = line.slice(5).trim();
                    if (chunk === "[DONE]") continue;
                    onToken(chunk);
                }
            }
        } else {
            // Plain chunked text: just emit as it comes
            onToken(partial);
            partial = "";
        }
    }
}

/**
 * Public send function: manages UI & state, chooses streaming or fallback.
 */
async function handleSend(text) {
    const userText = text.trim();
    if (!userText) return;

    // UI: render user message
    appendUserMessage(userText);

    // State
    messages.push({ role: "user", content: userText });

    // UI: prepare assistant message container (for streaming)
    const contentNode = appendAssistantMessage("");
    setSending(true);

    const payload = {
        messages, // full conversation
        // you can pass assistantId / tenantId / retrieval params here if needed
        // assistantId: "...",
        // rag: { topK: 6, filters: {...} },
    };

    try {
        let firstChunk = true;
        await sendChatStreaming(payload, (chunk) => {
            if (firstChunk) {
                contentNode.textContent = chunk;
                firstChunk = false;
            } else {
                contentNode.textContent += chunk;
            }
            autoscroll();
        });

        const finalText = contentNode.textContent || "";
        messages.push({ role: "assistant", content: finalText });
    } catch (err) {
        console.error("[chat] send error:", err);
        contentNode.textContent = (contentNode.textContent || "") + (contentNode.textContent ? "\n" : "") + "⚠️ " + err.message;
        toast("Chat error: " + err.message);
    } finally {
        setSending(false);
    }
}

// ---- Event wiring ----
function onSubmit(e) {
    e.preventDefault();
    if ($send()?.disabled) return;

    const v = $input().value;
    $input().value = "";
    handleSend(v);
}

function onKey(e) {
    if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        $send().click();
    }
}

function onStop() {
    try {
        abortController?.abort();
    } catch {}
    setSending(false);
}

function onClear() {
    clearChat();
    toast("Conversation cleared");
}

// ---- Init ----
(function init() {
    // composer
    document.getElementById("composer")?.addEventListener("submit", onSubmit);
    $input()?.addEventListener("keydown", onKey);
    $send()?.addEventListener("click", () => {
        const text = $input().value;
        if (!text.trim()) return;
        // the submit handler handles it, but in case:
        // handleSend(text.trim());
    });
    $stop()?.addEventListener("click", onStop);
    $clear()?.addEventListener("click", onClear);

    // optional: preload a system prompt or greeting in state
    // messages.push({ role: "system", content: "You are a helpful assistant." });
})();
