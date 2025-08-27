// voice.js (streaming upgrade)
const API_ASK_URL = "/api/chat/ask2";           // sync fallback
const API_STREAM_URL = "/api/chat/stream?q=";  // SSE GET with query param

const els = {
  conv: document.getElementById("conversation"),
  mic: document.getElementById("micBtn"),
  wave: document.getElementById("wave"),
  lang: document.getElementById("lang"),
  autoTts: document.getElementById("autoTts"),
  clear: document.getElementById("clearBtn"),
  quick: document.getElementById("quick"),
  wake: document.getElementById("wakeWord"),
};

let recognizing = false, recognition;
let mediaStream, audioCtx, analyser, dataArray, rafId;
let speaking = false;
let currentStream; // EventSource for SSE
let currentBotBubble; // DOM node that we keep appending tokens into

function addMsg(role, text){
  const wrap = document.createElement("div");
  wrap.className = `msg ${role}`;
  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = text;
  const meta = document.createElement("div");
  meta.className = "meta";
  meta.textContent = role === "user" ? "You" : "Assistant";
  wrap.appendChild(bubble); wrap.appendChild(meta);
  els.conv.appendChild(wrap);
  els.conv.scrollTop = els.conv.scrollHeight;
  return bubble;
}
function setMicLabel(label){ els.mic.textContent = label; }

// ---- Streaming ----
function startStream(question){
  // stop any ongoing TTS (barge-in)
  window.speechSynthesis.cancel();

  // Close old stream
  if (currentStream) { currentStream.close(); currentStream = null; }

  // Create bot bubble immediately and append tokens into it
  const bubble = addMsg("bot", "");
  currentBotBubble = bubble;

  const url = API_STREAM_URL + encodeURIComponent(question);
  const es = new EventSource(url);
  currentStream = es;

  let assembled = "";

  es.addEventListener("token", (ev) => {
    try {
      // data may be bytes; decode safely
      let chunk = ev.data || "";
      assembled += chunk;
      bubble.textContent = assembled;
      els.conv.scrollTop = els.conv.scrollHeight;
    } catch (e) { console.warn(e); }
  });

  es.addEventListener("done", () => {
    es.close();
    currentStream = null;
    if (els.autoTts.checked && assembled.trim()) {
      speak(assembled.trim());
    }
  });

  es.addEventListener("error", (ev) => {
    console.warn("SSE error", ev);
    es.close();
    currentStream = null;
    if (!assembled) bubble.textContent = "Sorry, streaming failed.";
  });
}

// ---- Fallback non-streaming ----
async function askAssistant(message){
    const controller = new AbortController();
    const timeoutMs = 20000; // 20s
    const to = setTimeout(() => controller.abort(new DOMException("Timeout", "AbortError")), timeoutMs);
  
    try{
      const res = await fetch("/api/chat/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message }),
        signal: controller.signal,
        cache: "no-store",
        keepalive: false
      });
  
      clearTimeout(to);
  
      if (!res.ok) {
        const text = await res.text().catch(()=> "");
        throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
      }
  
      // guard invalid JSON (if backend accidentally returns stray chars)
      let data;
      try {
        data = await res.json();
      } catch (e) {
        throw new Error("Invalid JSON from server");
      }
  
      // normalize to { answer, sources?, suggestions? }
      if (data && data.type === "rag_response") {
        return {
          answer: data.answer || "I don't have enough context yet.",
          sources: Array.isArray(data.sources) ? data.sources : [],
          suggestions: Array.isArray(data.suggestions) ? data.suggestions : []
        };
      }
      return { answer: data.reply || (typeof data === "string" ? data : JSON.stringify(data)) };
  
    } catch (err) {
      clearTimeout(to);
  
      // Friendly messages for common causes
      if (err?.name === "AbortError") {
        return { answer: "Request cancelled or timed out. Please try again." };
      }
      if (err?.message?.includes("Failed to fetch")) {
        return { answer: "Network error. Check server is running and CORS is allowed." };
      }
      return { answer: `Sorry, the chat failed: ${err?.message || "unknown error"}` };
    }
  }
  

// ---- Speech Recognition ----
function initRecognition(){
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if(!SR){ alert("Speech Recognition not supported in this browser. Try Chrome."); return null; }
  const rec = new SR();
  rec.lang = els.lang.value || "en-IN";
  rec.interimResults = true;
  rec.continuous = true;
  rec.maxAlternatives = 1;
  rec.onstart = ()=>{ recognizing = true; setMicLabel("Listening…"); };
  rec.onerror = (e)=> console.warn("recog error", e);
  rec.onend = ()=>{ recognizing = false; setMicLabel("Hold to Speak"); stopWave(); };
  rec.onresult = (ev)=>{
    let finalText = "", interim = "";
    for(let i=ev.resultIndex; i<ev.results.length; i++){
      const res = ev.results[i], txt = res[0].transcript;
      if(res.isFinal) finalText += txt; else interim += txt;
    }
    if(interim) showLiveTranscript(interim);
    if(finalText.trim()){
      clearLiveTranscript();
      handleUserText(finalText.trim());
    }
  };
  return rec;
}

let liveEl;
function showLiveTranscript(text){
  if(!liveEl){
    liveEl = document.createElement("div");
    liveEl.className = "msg user";
    const bubble = document.createElement("div");
    bubble.className = "bubble"; bubble.style.opacity = .8; bubble.id = "liveBubble";
    bubble.textContent = text;
    liveEl.appendChild(bubble);
    els.conv.appendChild(liveEl);
    els.conv.scrollTop = els.conv.scrollHeight;
  } else {
    document.getElementById("liveBubble").textContent = text;
  }
}
function clearLiveTranscript(){ if(liveEl){ liveEl.remove(); liveEl=null; } }

// ---- Mic / waveform ----
async function startMic(){
  if(mediaStream) return;
  try{
    mediaStream = await navigator.mediaDevices.getUserMedia({audio:true});
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const source = audioCtx.createMediaStreamSource(mediaStream);
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 512;
    const bufferLength = analyser.frequencyBinCount;
    dataArray = new Uint8Array(bufferLength);
    source.connect(analyser);
    drawWave();
  }catch(err){
    console.error(err);
    alert("Microphone permission denied.");
  }
}
function stopMic(){ if(mediaStream){ mediaStream.getTracks().forEach(t=>t.stop()); mediaStream=null; } stopWave(); }
function drawWave(){
  const ctx = els.wave.getContext("2d");
  function loop(){
    rafId = requestAnimationFrame(loop);
    analyser.getByteTimeDomainData(dataArray);
    ctx.clearRect(0,0,els.wave.width,els.wave.height);
    ctx.lineWidth = 2; ctx.strokeStyle = "#7c9cff"; ctx.beginPath();
    const slice = els.wave.width / dataArray.length;
    let x=0;
    for(let i=0;i<dataArray.length;i++){
      const v = dataArray[i] / 128.0;
      const y = (v * els.wave.height)/2;
      if(i===0) ctx.moveTo(x,y); else ctx.lineTo(x,y);
      x += slice;
    }
    ctx.lineTo(els.wave.width, els.wave.height/2);
    ctx.stroke();
  }
  loop();
}
function stopWave(){ if(rafId) cancelAnimationFrame(rafId); const ctx=els.wave.getContext("2d"); ctx.clearRect(0,0,els.wave.width,els.wave.height); }

// ---- TTS ----
function speak(text){
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = els.lang.value || "en-IN";
  speaking = true;
  u.onend = ()=> speaking = false;
  speechSynthesis.speak(u);
}

// ---- Flow ----
async function handleUserText(text){
  const ww = (els.wake.value||"").trim().toLowerCase();
  if(ww && !text.toLowerCase().includes(ww)) return;

  addMsg("user", text);

  // Prefer SSE streaming; fall back to POST if EventSource unsupported
  if ("EventSource" in window) {
    startStream(text);
  } else {
    const reply = await askAssistant(text);
    addMsg("bot", reply);
    if (els.autoTts.checked) speak(normalizeAnswer(reply.answer || ""));
  }
}

// ---- Events ----
els.mic.addEventListener("mousedown", async ()=>{
  await startMic();
  recognition = recognition || initRecognition();
  if(recognition && !recognizing){ recognition.lang = els.lang.value || "en-IN"; recognition.start(); }
});
els.mic.addEventListener("mouseup", ()=>{ if(recognition && recognizing){ recognition.stop(); } });
els.mic.addEventListener("mouseleave", ()=>{ if(recognition && recognizing){ recognition.stop(); } });

els.clear.addEventListener("click", ()=>{
  // stop stream & TTS
  if (currentStream) { currentStream.close(); currentStream = null; }
  window.speechSynthesis.cancel();
  els.conv.innerHTML = "";
});

els.quick.addEventListener("click", (e)=>{ if(e.target.tagName==="BUTTON"){ handleUserText(e.target.textContent); } });

// init
addMsg("bot","Voice mode (streaming) ready. Hold the mic and ask about products, sales, or inventory.");
function normalizeAnswer(s="") {
    if (!s) return s;
  
    // Convert non-breaking spaces & weird unicode to regular space
    s = s.replace(/\u00A0|\u2007|\u202F/g, " ");
  
    // If the string has literally no spaces, try to reinsert after punctuation/bullets
    if (!/\s/.test(s)) {
      s = s
        .replace(/([.,;:!?])(?=\S)/g, "$1 ")   // add a space after punctuation if missing
        .replace(/\*(?=\S)/g, "* ");           // bullets like "*Point" -> "* Point"
    }
  
    // Collapse runaway whitespace to single spaces, but preserve newlines
    s = s.replace(/[ \t]+/g, " ");
  
    // Tidy up lists: "-item" -> "- item"
    s = s.replace(/([*-])(?=\S)/g, "$1 ");
  
    return s.trim();
  }
  function renderAnswerRich(raw){
    const s = normalizeAnswer(raw || "");
    const container = document.createElement("div");
  
    // naive bullet handling: lines starting with "*" or "-" → list
    const lines = s.split(/\n/);
    let ul;
    lines.forEach(line => {
      if (/^\s*[*-]\s+/.test(line)) {
        if (!ul) { ul = document.createElement("ul"); container.appendChild(ul); }
        const li = document.createElement("li");
        li.textContent = line.replace(/^\s*[*-]\s+/, "");
        ul.appendChild(li);
      } else {
        if (ul) ul = null; // stop list on plain line
        const p = document.createElement("p");
        p.textContent = line;
        container.appendChild(p);
      }
    });
    return container;
  }
  
  // then in handleUserText:
  const frag = document.createDocumentFragment();
  frag.appendChild(renderAnswerRich(resp.answer));
  
  function normalizeAnswer(s=""){
    // convert weird unicode spaces to regular spaces
    s = s.replace(/\u00A0|\u2007|\u202F/g, " ");
  
    // if no whitespace at all → repair
    if (!/\s/.test(s)) {
      s = s
        .replace(/([.,;:!?])(?!\s)/g, "$1 ")   // add space after punctuation
        .replace(/(?<=[a-z])(?=[A-Z])/g, " ")  // split lower→Upper
        .replace(/(?<=[A-Za-z])(?=\d)/g, " ")  // letter→digit
        .replace(/(?<=\d)(?=[A-Za-z])/g, " ");
    }
    // collapse runs but keep line breaks
    s = s.replace(/[ \t]{2,}/g, " ").trim();
    return s;
  }
  
  // use it when displaying & TTS:
  ans.textContent = normalizeAnswer(resp.answer || " ");
  speak(normalizeAnswer(resp.answer || ""));
  