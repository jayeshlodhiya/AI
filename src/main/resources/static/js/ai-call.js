// ===== Config =====
const DEFAULT_API_BASE = window.location.origin;//"http://localhost:8082";
console.log("Base Url : ",DEFAULT_API_BASE);
const API_BASE = (window.API_BASE || DEFAULT_API_BASE).replace(/\/+$/,'');
const API_CALL = "/api/qcall/playground/call";
// If your API uses sessions/cookies, flip this to true
const USE_CREDENTIALS = false;

// ===== DOM getters =====
const statusText = () => document.getElementById("statusText");
const assistantsList = () => document.getElementById("assistantsList");
const emptyState = () => document.getElementById("emptyState");

// Create form
const createCard = () => document.getElementById("createCard");
const caName = () => document.getElementById("ca_name");
const caCompany = () => document.getElementById("ca_company");
const caVoice = () => document.getElementById("ca_voice");
const caPrompt = () => document.getElementById("ca_prompt");
const createStatus = () => document.getElementById("createStatus");

// ===== helpers =====
function el(tag, attrs = {}, ...children) {
    const n = document.createElement(tag);
    Object.entries(attrs).forEach(([k, v]) => {
        if (k === "class") n.className = v;
        else if (k === "html") n.innerHTML = v;
        else n.setAttribute(k, v);
    });
    children.flat().forEach((c) => {
        if (c == null) return;
        n.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return n;
}

// fetch with timeout + diagnostics
async function jsonFetch(url, opts = {}) {
    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), 15000); // 15s
    const finalOpts = {
        method: "GET",
        headers: { "Accept": "application/json", ...(opts.headers||{}) },
        credentials: USE_CREDENTIALS ? "include" : "same-origin",
        mode: "cors",
        signal: controller.signal,
        ...opts
    };

    console.log("[FETCH]", finalOpts.method || "GET", url, finalOpts);

    try {
        const r = await fetch(url, finalOpts);
        clearTimeout(t);
        if (!r.ok) {
            const txt = await r.text().catch(() => "");
            console.error("[FETCH][HTTP ERROR]", r.status, txt);
            throw new Error(`HTTP ${r.status}${txt ? `: ${txt}` : ""}`);
        }
        const ct = r.headers.get("content-type") || "";
        if (ct.includes("application/json")) {
            return await r.json();
        }
        // not json? return raw text for debugging
        const txt = await r.text();
        console.warn("[FETCH] Non-JSON response:", txt.slice(0, 300));
        try { return JSON.parse(txt); } catch { return { raw: txt }; }
    } catch (err) {
        clearTimeout(t);
        if (err.name === "AbortError") {
            console.error("[FETCH][TIMEOUT]", url);
            throw new Error("Request timeout (15s)");
        }
        if (String(err).includes("Failed to fetch")) {
            console.error("[FETCH][CORS/MIXED-CONTENT?] Check API_BASE, http/https, and CORS headers.");
            throw new Error("Network/CORS error (see console)");
        }
        throw err;
    }
}

function assistantIdOf(a) {
    return a.id || a._id || a.assistantId || a.knowledge_base_Id || a.name;
}

function toast(message) {
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.position = "fixed";
        container.style.bottom = "20px";
        container.style.right = "20px";
        container.style.zIndex = "9999";
        document.body.appendChild(container);
    }
    const toastEl = document.createElement("div");
    toastEl.innerText = message;
    toastEl.style.background = "#333";
    toastEl.style.color = "#fff";
    toastEl.style.padding = "10px 20px";
    toastEl.style.marginTop = "10px";
    toastEl.style.borderRadius = "8px";
    toastEl.style.boxShadow = "0 4px 6px rgba(0,0,0,0.2)";
    toastEl.style.opacity = "1";
    toastEl.style.transition = "opacity 0.5s ease";
    container.appendChild(toastEl);
    setTimeout(() => {
        toastEl.style.opacity = "0";
        setTimeout(() => toastEl.remove(), 500);
    }, 3000);
}

// ===== Assistants render =====
function renderAssistants(list) {
    const host = assistantsList();
    const empty = emptyState();
    const count = document.getElementById("countPill");
    const tmpl = document.getElementById("assistantCardTemplate");

    if (!host || !tmpl) return;
    host.innerHTML = "";

    if (Array.isArray(list) && list.length) {
        if (count) { count.textContent = list.length; count.style.display = "inline-block"; }
    } else {
        if (count) count.style.display = "none";
    }

    if (!Array.isArray(list) || list.length === 0) {
        if (empty) empty.style.display = "block";
        return;
    }
    if (empty) empty.style.display = "none";

    list.forEach((a) => {
        const id = assistantIdOf(a);
        const node = tmpl.content.firstElementChild.cloneNode(true);

        node.dataset.assistantId = id || "";
        const avatar = node.querySelector('[data-role="avatar"]');
        if (avatar) avatar.src = a.avatar_url || a.image || a.avatarUrl || "https://placehold.co/112x112";

        const nameEl = node.querySelector('[data-role="name"]');
        if (nameEl) nameEl.textContent = a.name || a.title || "Assistant";

        const dirEl = node.querySelector('[data-role="direction"]');
        if (dirEl) dirEl.textContent = (a.direction || a.type || "OUTBOUND").toUpperCase();

        const titleEl = node.querySelector('[data-role="title"]');
        if (titleEl) titleEl.textContent = a.title || a.use_case || a.description || "—";

        const companyEl = node.querySelector('[data-role="company"]');
        if (companyEl) companyEl.textContent = a.company_name || a.company || a.org || "—";

        const hName = node.querySelector('[data-role="hidden-name"]');
        const hPhone = node.querySelector('[data-role="hidden-phone"]');
        if (id) {
            if (hName) hName.id = `nm_${id}`;
            if (hPhone) hPhone.id = `ph_${id}`;
        }

        host.appendChild(node);
    });
}

// ===== API loaders =====
async function loadAssistants() {
    statusText()?.replaceChildren(el("span",{class:"spinner"})," Loading assistants…");
    try {
        const url = `${API_BASE}/api/qcall/assistants`;
        const assistants = await jsonFetch(url);
        renderAssistants(assistants || []);
        statusText() && (statusText().textContent = "Ready.");
    } catch (e) {
        console.error(e);
        statusText() && (statusText().textContent = "Failed to load assistants.");
        toast("Failed to load assistants (see console)");
    }
}

async function loadCallHistory() {
    try {
        const url = `${API_BASE}/api/qcall/playgroundHistory`;
        const callLogs = await jsonFetch(url);
        const rows = Array.isArray(callLogs) ? callLogs : (callLogs.calls || []);
        renderCallHistoryFromApi(rows);
    } catch (e) {
        console.error(e);
        statusText() && (statusText().textContent = "Failed to load call logs.");
        renderCallHistoryFromApi([]);
        toast("Failed to load call history (see console)");
    }
}

// Quick connectivity probe (optional)
// async function pingApi() { /* omitted for brevity */ }

// ===== Create assistant =====
function openCreate() {
    createStatus() && (createStatus().textContent = "");
    caName() && (caName().value = "");
    caCompany() && (caCompany().value = "");
    caVoice() && (caVoice().value = "");
    caPrompt() && (caPrompt().value = "");
    createCard() && (createCard().style.display = "block");
    window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
}
function cancelCreate() {
    createCard() && (createCard().style.display = "none");
}

async function createAssistant() {
    const name = (caName()?.value || "").trim();
    if (!name) { createStatus() && (createStatus().textContent = "Please provide an assistant name."); return; }
    const company = (caCompany()?.value || "").trim();
    const voice = (caVoice()?.value || "").trim();
    const prompt = (caPrompt()?.value || "").trim();

    createStatus() && (createStatus().innerHTML = `<span class="spinner"></span> Creating…`);

    try {
        const payload = { name, company_name: company || undefined, voice_name: voice || undefined, system_prompt: prompt || undefined };
        const url = `${API_BASE}/api/qcall/assistants`;
        await jsonFetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        createStatus() && (createStatus().textContent = "Assistant created.");
        setTimeout(() => { cancelCreate(); loadAssistants(); }, 600);
    } catch (e) {
        createStatus() && (createStatus().textContent = `Failed: ${e.message}`);
    }
}

// ===== Place call =====
async function playgroundCall(assistantId, btn){
    const nameEl  = document.getElementById(`nm_${assistantId}`);
    const phoneEl = document.getElementById(`ph_${assistantId}`);
    const fullName = (nameEl?.value || '').trim();
    const phone    = (phoneEl?.value || '').trim();

    if(!phone || !/^\+\d{8,15}$/.test(phone)){
        toast('Enter phone in E.164 format (e.g., +917350348860)');
        phoneEl?.focus(); return;
    }
    const [firstName,...rest]=(fullName||'Customer').split(/\s+/);
    const lastName=rest.join(' ')||'—';

    const payload={
        firstName,lastName,
        email:"noreply@example.com",
        assistantId,
        phoneNumber:[phone],
        dialerId:""
    };

    const old=btn.innerHTML;
    btn.disabled=true; btn.innerHTML=`<span class="spinner"></span> Calling…`;

    try{
        const url = `${API_BASE}${API_CALL}`;
        const r = await fetch(url, {
            method:'POST',
            headers:{'Content-Type':'application/json','Accept':'application/json'},
            credentials: USE_CREDENTIALS ? "include" : "same-origin",
            mode: "cors",
            body: JSON.stringify(payload)
        });
        const text=await r.text().catch(()=> "");
        console.log("[CALL][RESP]", r.status, text);
        if(!r.ok){
            toast(`Failed: HTTP ${r.status}`);
        }else{
            toast('Call initiated ✔');
            setTimeout(loadCallHistory, 1000);
        }
    }catch(err){
        console.error("[CALL][ERR]", err);
        toast('Network/CORS error (see console)');
    }finally{
        btn.disabled=false; btn.innerHTML=old;
    }
}

/* ----------------------------------------------------------------
   Transcript parsing + rendering (server-normalized friendly)
---------------------------------------------------------------- */

/**
 * Robustly parse transcript into an array of {type, text}.
 * Accepts:
 *  - Array<{type,text}>
 *  - single/double-escaped JSON string
 *  - raw JSON-ish string (regex fallback)
 */
function parseTranscript(raw) {
    if (raw == null) return null;
    //raw = formatTranscript(raw);
    // Already an array (server normalized)
    if (Array.isArray(raw)) return raw;

    // If object like { turns: [...] }
    if (typeof raw === 'object' && raw.turns && Array.isArray(raw.turns)) {
        return raw.turns;
    }

    // Attempt up to 3 parses for escaped strings
    let v = raw;
    for (let i = 0; i < 3; i++) {
        if (typeof v === "string") {
            const s = v.trim();
            // quick guard to avoid parsing pure text that isn't JSON-like
            const looksJson = s.startsWith('[') || s.startsWith('{') || s.includes('"type"') || s.includes('"text"');
            if (!looksJson) break;

            try {
                v = JSON.parse(s);
                if (Array.isArray(v)) return v;
                if (typeof v === 'object' && v.turns && Array.isArray(v.turns)) return v.turns;
                // If parse returns a string, try again
                if (typeof v === 'string') continue;
                // Parsed to non-array object without turns; stop
                break;
            } catch {
                // try next strategy
                break;
            }
        } else {
            break;
        }
    }

    // Regex fallback: extract {"type":"...","text":"..."}
    try {
        const s = String(raw);
        const matches = s.match(/\{[^{}]*"type"\s*:\s*"[^"]*"\s*,\s*"text"\s*:\s*"[^"]*"\s*\}/g);
        if (matches && matches.length) {
            const arr = [];
            for (const m of matches) {
                try { arr.push(JSON.parse(m)); } catch {}
            }
            if (arr.length) return arr;
        }
    } catch {}

    return null; // give up; UI will show raw text
}

/** Convert parsed transcript array into plain text lines */
function transcriptToPlainText(turns) {
    if (!Array.isArray(turns)) return null;
    return turns.map(t => {
        const who = (t?.type || "").toLowerCase();
        const label = (who === "user" || who === "human" || who === "customer") ? "User" : "Assistant";
        return `${label}: ${t?.text ?? ""}`.trim();
    }).join("\n");
}

/** Build a <pre> node with plain text transcript (fallback to raw string) */
function transcriptPreNode(raw) {
    const turns = parseTranscript(raw);
    const text = turns ? transcriptToPlainText(turns) : String(raw ?? "—");
    const pre = document.createElement("pre");
    pre.style.whiteSpace = "pre-wrap";
    pre.style.margin = "0";
    pre.textContent = formatTranscript(text);
    return pre;
}

/* ----------------------------------------------------------------
   Modal
---------------------------------------------------------------- */
const callModal = {
    el: null, body: null, title: null, closeBtn: null, closeBtn2: null,
    init() {
        this.el = document.getElementById('callDetailModal');
        this.body = document.getElementById('callDetailContent');
        this.title = document.getElementById('callDetailTitle');
        this.closeBtn = document.getElementById('callDetailClose');
        this.closeBtn2 = document.getElementById('callDetailClose2');
        if (!this.el) return;
        const close = () => this.hide();
        this.closeBtn?.addEventListener('click', close);
        this.closeBtn2?.addEventListener('click', close);
        this.el.querySelector('.modal__backdrop')?.addEventListener('click', close);
        document.addEventListener('keydown', (e) => {
            if (this.el?.classList.contains('hidden')) return;
            if (e.key === 'Escape') close();
        });
    },
    show(){ this.el && (this.el.classList.remove('hidden'), this.el.setAttribute('aria-hidden','false')); },
    hide(){ this.el && (this.el.classList.add('hidden'), this.el.setAttribute('aria-hidden','true')); },
    renderKV(pairs){
        if (!this.body) return;
        this.body.innerHTML = '';
        const frag = document.createDocumentFragment();
        pairs.forEach((row) => {
            if (row === 'sep') { const sep=document.createElement('div'); sep.className='sep'; frag.appendChild(sep); return; }
            const k = document.createElement('div'); k.className='k'; k.textContent=row.k;
            const v = document.createElement('div'); v.className='v';
            if (row.node) v.appendChild(row.node);
            else if (row.htmlCodeJson) { const code=document.createElement('code'); code.className='json'; code.textContent=row.htmlCodeJson; v.appendChild(code); }
            else v.textContent=row.v;
            frag.appendChild(k); frag.appendChild(v);
        });
        this.body.appendChild(frag);
    }
};
callModal.init();

// small helpers
function safeString(v) {
    if (v === null || v === undefined) return '—';
    if (typeof v === 'object') return JSON.stringify(v);
    return String(v);
}
function fmtDateTimeISO(v) {
    if (!v && v !== 0) return '—';
    try {
        const d = typeof v === 'number' ? new Date(v) : new Date(String(v));
        if (isNaN(d)) return '—';
        return d.toLocaleString();
    } catch { return '—'; }
}
function prettyJsonMaybe(str) {
    try { const obj = typeof str === 'string' ? JSON.parse(str) : str; return JSON.stringify(obj, null, 2); }
    catch { return typeof str === 'string' ? str : JSON.stringify(str, null, 2); }
}

/* ----------------------------------------------------------------
   Modal content builder (Transcript now uses parser)
---------------------------------------------------------------- */
function openCallDetail(record, opts = { autoplay: false }) {
    if (!record) return;

    if (callModal.title) callModal.title.textContent = `Call ${record.id || '—'}`;

    const kv = [
        { k: 'ID', v: safeString(record.id) },
        { k: 'Assistant', v: record.assistant_name || '—' },
        { k: 'Phone Number', v: record.phone_number || '—' },
        { k: 'Created At', v: fmtDateTimeISO(record.created_at) },
        'sep',
        { k: 'Call Cost', v: record.call_cost != null ? String(record.call_cost) : '—' },
        { k: 'Duration (sec)', v: record.call_duration_in_sec != null ? String(record.call_duration_in_sec) : '—' },
        { k: 'Sentiment', v: record.call_sentiment == null ? '—' : safeString(record.call_sentiment) },
        { k: 'Call SID', v: record.call_sid || '—' },
        { k: 'Goal', v: record.goal || '—' },
        'sep'
    ];

    // ▶️ Recording (audio player + download), only if present
    if (record.recording_url) {
        const wrap = document.createElement('div');
        wrap.style.display = 'grid';
        wrap.style.gap = '8px';
        const audio = document.createElement('audio');
        audio.controls = true;
        audio.src = record.recording_url; // prefix with API_BASE if needed
        audio.id = 'callAudioPlayer';
        audio.style.width = '100%';
        const link = document.createElement('a');
        link.href = audio.src;
        link.download = '';
        link.textContent = 'Download recording';
        wrap.appendChild(audio);
        wrap.appendChild(link);
        kv.push({ k: 'Recording', node: wrap });
    } else {
        kv.push({ k: 'Recording', v: '—' });
    }

    // 📝 Transcript (PLAIN TEXT in <pre>, parsed!)
    if (record.call_transcribe != null) {
        kv.push({ k: "Transcript", node: transcriptPreNode(record.call_transcribe) }, "sep");
    } else {
        kv.push({ k: "Transcript", v: "—" }, "sep");
    }

    // Operational JSONs and flags
    kv.push(
        { k: 'Actions', htmlCodeJson: prettyJsonMaybe(record.actions) },
        { k: 'Webhook Response', htmlCodeJson: prettyJsonMaybe(record.webhook_response) },
        { k: 'GPT Response', htmlCodeJson: prettyJsonMaybe(record.gpt_response) },
        'sep',
        { k: 'Transferred?', v: String(!!record.is_call_transferred) },
        { k: 'Recording On?', v: String(!!record.is_recording) },
        { k: 'Webhook Triggered?', v: String(!!record.is_webhook) },
        { k: 'Email Sent?', v: String(!!record.is_email_sent) },
        { k: 'Meeting Scheduled?', v: String(!!record.is_meeting_scheduled) },
        { k: 'Outcome', v: record.outcome == null ? '—' : safeString(record.outcome) }
    );

    callModal.renderKV(kv);
    callModal.show();

    if (opts.autoplay) {
        setTimeout(() => {
            const audio = document.getElementById('callAudioPlayer');
            if (audio) {
                const p = audio.play();
                if (p && typeof p.catch === 'function') p.catch(() => {});
            }
        }, 80);
    }
}

/* ----------------------------------------------------------------
   Call history table
---------------------------------------------------------------- */
function renderCallHistoryFromApi(apiRows) {
    const tbody = document.getElementById('callsTbody');
    if (!tbody) return;

    // Ensure table header (optional if static in HTML)
    const thead = tbody.closest('table')?.querySelector('thead tr');
    if (thead) {
        thead.innerHTML = `
      <th style="min-width:220px">ID</th>
      <th style="min-width:140px">Phone</th>
      <th style="min-width:100px">Cost</th>
      <th style="min-width:120px">Duration (sec)</th>
      <th style="min-width:120px">Sentiment</th>
      <th style="min-width:160px">Created At</th>
      <th style="min-width:160px">Assistant</th>
      <th style="min-width:100px; text-align:right;">Actions</th>
    `;
    }

    tbody.innerHTML = '';

    (apiRows || []).forEach((rec) => {
        const tr = document.createElement('tr');
        tr._data = rec;

        const idCell = document.createElement('td'); idCell.textContent = safeString(rec.id);
        const phoneCell = document.createElement('td'); phoneCell.textContent = rec.phone_number || '—';
        const costCell = document.createElement('td'); costCell.textContent = rec.call_cost != null ? String(rec.call_cost) : '—';
        const durCell = document.createElement('td'); durCell.textContent = rec.call_duration_in_sec != null ? String(rec.call_duration_in_sec) : '—';
        const sentCell = document.createElement('td'); sentCell.textContent = rec.call_sentiment == null ? '—' : safeString(rec.call_sentiment);
        const createdCell = document.createElement('td'); createdCell.textContent = fmtDateTimeISO(rec.created_at);
        const asstCell = document.createElement('td'); asstCell.textContent = rec.assistant_name || '—';

        const actionsCell = document.createElement('td');
        actionsCell.style.textAlign = 'right';
        const btn = document.createElement('button');
        btn.className = 'btn-lite';
        btn.setAttribute('data-action', 'play-recording');
        btn.title = rec.recording_url ? 'Play recording' : 'No recording';
        btn.innerHTML = rec.recording_url ? '▶️ Play' : '—';
        btn.disabled = !rec.recording_url;
        actionsCell.appendChild(btn);

        tr.append(idCell, phoneCell, costCell, durCell, sentCell, createdCell, asstCell, actionsCell);
        tbody.appendChild(tr);
    });

    const pageInfo = document.getElementById('pageInfo');
    if (pageInfo) {
        const total = (apiRows || []).length;
        pageInfo.textContent = `Showing ${total} call${total !== 1 ? 's' : ''}`;
    }
}

// Delegated click: play button -> open modal & autoplay
(function bindPlayButtons(){
    const tbody = document.getElementById('callsTbody');
    if (!tbody) return;
    tbody.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-action="play-recording"]');
        if (!btn) return;
        const tr = btn.closest('tr');
        if (!tr || !tr._data) return;
        openCallDetail(tr._data, { autoplay: true });
    });
})();

// Delegated click: open modal on row click (skip play button)
(function bindCallHistoryRowClicks(){
    const tbody = document.getElementById('callsTbody');
    if (!tbody) return;
    tbody.addEventListener('click', (e) => {
        if (e.target.closest('[data-action="play-recording"]')) return;
        const tr = e.target.closest('tr');
        if (!tr || !tr._data) return;
        openCallDetail(tr._data, { autoplay: false });
    });
})();

// ===== Wire up after DOM is ready =====
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("refreshBtn")?.addEventListener("click", () => { loadAssistants(); loadCallHistory(); });
    document.getElementById("openCreateBtn")?.addEventListener("click", openCreate);
    document.getElementById("cancelCreateBtn")?.addEventListener("click", cancelCreate);
    document.getElementById("createBtn")?.addEventListener("click", createAssistant);

    // Handler for "Call" buttons on assistant cards
    document.addEventListener("click", (e) => {
        const btn = e.target.closest('[data-action="assistant-call"]');
        if (!btn) return;
        const card = btn.closest(".assistant-card");
        const id = card?.dataset.assistantId;
        if (!id) { toast("Missing assistant id on card."); return; }
        const hName = card.querySelector('[data-role="hidden-name"]');
        const hPhone = card.querySelector('[data-role="hidden-phone"]');
        const fallbackName = document.getElementById("nm_qt")?.value || "";
        let fallbackPhone = document.getElementById("ph_qt")?.value || "";
        if (!fallbackPhone) fallbackPhone = window.prompt("Enter phone in E.164 format (e.g., +917350348860)") || "";
        if (hName) { hName.id = `nm_${id}`; hName.value = fallbackName; }
        if (hPhone) { hPhone.id = `ph_${id}`; hPhone.value = fallbackPhone; }
        playgroundCall(id, btn);
    });

    loadAssistants();
    loadCallHistory();
});
function formatTranscript(raw) {
    if (!raw) return "—";

    let str = raw;

    try {
        // If not starting with [ but contains [{\" → wrap it
        if (typeof str === "string") {
            str = str.trim();

            // Case 1: already a valid JSON array string
            if (str.startsWith("[{")) {
                // replace \" with " to fix escaping
                str = str.replace(/\\"/g, '"');
            }

            // Case 2: it might still be wrapped in quotes (e.g. "\"[...]\"")
            if (str.startsWith("\"") && str.endsWith("\"")) {
                str = JSON.parse(str); // unwrap once
            }
        }

        const arr = JSON.parse(str);

        if (!Array.isArray(arr)) return String(raw);

        return arr.map(turn => {
            const who = (turn.type || "").toLowerCase();
            const label = (who === "user" || who === "customer" || who === "human")
                ? "User"
                : "Assistant";
            return `${label}: ${turn.text}`;
        }).join("\n");
    } catch (err) {
        console.error("Parse error:", err, raw);
        return String(raw);
    }
}

