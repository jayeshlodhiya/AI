const VOICE_PAUSE_MS = 2500;  // 2.5s
async function compressImage(file, targetMaxBytes = 1024*1024) {
    // Load into <img>
    const dataURL = await new Promise((res, rej) => {
        const r = new FileReader();
        r.onload = () => res(r.result);
        r.onerror = rej;
        r.readAsDataURL(file);
    });

    const img = await new Promise((res, rej) => {
        const el = new Image();
        el.onload = () => res(el);
        el.onerror = rej;
        el.src = dataURL;
    });

  //  prev.src = dataURL;
  //  origInfo.textContent = `Original: ${img.width}×${img.height}, ${human(file.size)}`;

    // Draw to canvas (downscale if needed)
    function drawToCanvas(img,  maxW=1600, maxH=1600) {
        const ratio = Math.min(1, maxW / img.width, maxH / img.height);
        const w = Math.round(img.width * ratio);
        const h = Math.round(img.height * ratio);
        const canvas = document.createElement('canvas');
        canvas.width = w; canvas.height = h;
        const g = canvas.getContext('2d');
        // simple background to avoid black where transparent
        g.fillStyle = '#fff'; g.fillRect(0,0,w,h);
        g.drawImage(img, 0, 0, w, h);
        return canvas;
    }

    //const canvas = drawToCanvas(img, 1600, 1600);
    const canvas = drawToCanvas(img, 1600, 1600);
    // Try descending JPEG qualities until < 1MB (or very low)
    let q = 0.72, blob;
    for (; q >= 0.4; q -= 0.06) {
        blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', q));
        if (blob && blob.size <= targetMaxBytes) break;
    }
    if (!blob) {
      //  msg.textContent = 'Compression failed.';
        return null;
    }
    //compInfo.textContent = `Compressed: ${canvas.width}×${canvas.height}, ${human(blob.size)} (q≈${q.toFixed(2)})`;
    return blob;
}

function UploadImg( blob){
    const fd = new FormData();
    fd.append('file', new File([blob], name, { type: 'image/jpeg/png' }));
    fd.append('tenant_id', 'demo');
    const xhr = new XMLHttpRequest();
    xhr.open('POST', API_BASE + '/api/ingest/scan', true);
    try {
        xhr.send(fd);
    }catch (err){

    }


}
async function convertFileType(file, targetType = "jpg") {
    return new Promise((resolve, reject) => {
        // If not PNG, return the original file
        if (!file.type.includes("png")) {
            resolve(file);
            return;
        }

        const img = new Image();
        img.src = URL.createObjectURL(file);

        img.onload = () => {
            const canvas = document.createElement("canvas");
            canvas.width = img.width;
            canvas.height = img.height;

            const ctx = canvas.getContext("2d");
            ctx.fillStyle = "#ffffff"; // Replace transparency with white
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            ctx.drawImage(img, 0, 0);

            // Convert to JPG Blob
         return    canvas.toBlob(
                (blob) => {
                    if (!blob) {
                        reject(new Error("Failed to convert image."));
                        return;
                    }
                    const newFile = new File(
                        [blob],
                        file.name.replace(/\.[^/.]+$/, "") + `.${targetType}`,
                        { type: `image/${targetType}` }
                    );
                    resolve(newFile);
                },
                `image/${targetType}`,
                0.9 // quality
            );
        };

        img.onerror = (err) => reject(err);
    });
}
let rec = null;
let isListening = false;
let mediaStream = null;     // for backend fallback
const voiceStatus = document.getElementById('voiceStatus');

function isWebSpeechSTTSupported(){
    return 'webkitSpeechRecognition' in window || 'SpeechRecognition' in window;
}

function getLang(){
    return (document.getElementById('voiceLang')?.value) || 'hi-IN';
}

function setVoiceStatus(msg){
    if (!voiceStatus) return;
    voiceStatus.textContent = msg || '';
}

function toggleVoice(){
    if (isListening) {
        stopVoice();
    } else {
        startVoice();
    }
}

function startVoice(){
    const lang = getLang();
    // Prefer browser STT if supported
    if (isWebSpeechSTTSupported()){
        startWebSpeechSTT(lang);
    } else {
        // Fallback to backend recorder (WebRTC/MediaRecorder → /api/stt)
        startBackendRecorder(lang);
    }
}

function stopVoice(){
    if (rec) {
        try { rec.stop(); } catch(_) {}
    }
    if (mediaStream) {
        mediaStream.getTracks().forEach(t=>t.stop());
        mediaStream = null;
    }
    document.getElementById('micBtn')?.classList.remove('listening');
    isListening = false;
    setVoiceStatus('');
}
let pauseTimer = null;
let userForcedStop = false;
/* -------- A) Browser STT (Web Speech API) -------- */
function startWebSpeechSTT(lang){
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    rec = new SR();
    rec.lang = lang;
    rec.interimResults = true;
    rec.continuous = true; // keep stream alive between short pauses

    const micBtn = document.getElementById('micBtn');
    micBtn?.classList.add('listening');
    isListening = true;
    userForcedStop = false;
    setVoiceStatus(`🎧 Listening (${lang})… pause briefly to keep going, long pause to send`);

    let finalText = '';

    function scheduleFinish() {
        clearTimeout(pauseTimer);
        pauseTimer = setTimeout(() => {
            // Long pause detected → stop & send
            stopVoice();          // will call rec.stop()
            const input = document.getElementById('chatInput');
            const text = (input.value || '').trim();
            if (text) sendChat();
        }, VOICE_PAUSE_MS);
    }

    rec.onresult = (e) => {
        let interim = '';
        for (let i = e.resultIndex; i < e.results.length; i++){
            const t = e.results[i][0].transcript;
            if (e.results[i].isFinal) finalText += t + ' ';
            else interim += t;
        }
        const input = document.getElementById('chatInput');
        input.value = (finalText || interim || '').trim();

        // Each time we get speech, reset the long-pause timer
        scheduleFinish();
    };

    rec.onerror = (e) => {
        setVoiceStatus('⚠️ Mic error: ' + (e.error || 'unknown'));
        stopVoice();
    };

    rec.onend = () => {
        clearTimeout(pauseTimer);
        // If user didn’t force stop and we didn’t send yet, restart to keep session alive
        if (isListening && !userForcedStop) {
            try { rec.start(); } catch(_) {}
            // Re-arm the long pause timer so it can finish on silence
            scheduleFinish();
        }
    };

    rec.start();
}

function stopVoice(){
    userForcedStop = true;
    clearTimeout(pauseTimer);
    if (rec) {
        try { rec.stop(); } catch(_) {}
    }
    if (mediaStream) {
        mediaStream.getTracks().forEach(t=>t.stop());
        mediaStream = null;
    }
    document.getElementById('micBtn')?.classList.remove('listening');
    isListening = false;
    setVoiceStatus('');
}

/* -------- B) Backend STT fallback (MediaRecorder → /api/stt) --------
   Your backend should accept audio/webm (Opus) or audio/wav and return { text: "..." }.
*/
let recorder, recordedChunks = [];
const SILENCE_DB = -50;      // threshold in dBFS (approx)
const SILENCE_MS = 2500;     // need 2.5s of silence to stop
let analyser, audioCtx, sourceNode, silenceTimerId;

async function startBackendRecorder(lang){
    try{
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch(err){
        setVoiceStatus('⚠️ Mic permission denied');
        return;
    }

    // Init recorder
    recordedChunks = [];
    recorder = new MediaRecorder(mediaStream, { mimeType: 'audio/webm' });
    recorder.ondataavailable = (e)=>{ if (e.data.size > 0) recordedChunks.push(e.data); };
    recorder.onstop = async ()=>{
        const blob = new Blob(recordedChunks, { type: 'audio/webm' });
        await uploadForSTT(blob, lang);
        stopVoice();
    };

    // Init analyser for silence detection
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    sourceNode = audioCtx.createMediaStreamSource(mediaStream);
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 2048;
    sourceNode.connect(analyser);

    // Start
    recorder.start();
    isListening = true;
    document.getElementById('micBtn')?.classList.add('listening');
    setVoiceStatus(`🎧 Listening (${lang})… long pause to auto-send`);

    monitorSilence();
}

function monitorSilence(){
    const buffer = new Float32Array(analyser.fftSize);

    function check() {
        analyser.getFloatTimeDomainData(buffer);
        // RMS energy
        let sum = 0;
        for (let i=0; i<buffer.length; i++) sum += buffer[i]*buffer[i];
        const rms = Math.sqrt(sum / buffer.length);
        const db = 20 * Math.log10(rms + 1e-12); // avoid -Inf

        if (db < SILENCE_DB) {
            // Start/continue silence window
            if (!silenceTimerId) {
                silenceTimerId = setTimeout(() => {
                    // sustained silence -> stop & send
                    try { recorder.stop(); } catch(_) {}
                }, SILENCE_MS);
            }
        } else {
            // Noise/speech detected -> reset timer
            if (silenceTimerId) {
                clearTimeout(silenceTimerId);
                silenceTimerId = null;
            }
        }

        if (isListening) requestAnimationFrame(check);
    }
    requestAnimationFrame(check);
}

function stopVoice(){
    userForcedStop = true;
    clearTimeout(pauseTimer);
    if (silenceTimerId) { clearTimeout(silenceTimerId); silenceTimerId = null; }
    if (rec) { try { rec.stop(); } catch(_) {} }
    if (recorder && recorder.state !== 'inactive') { try { recorder.stop(); } catch(_) {} }
    if (mediaStream) { mediaStream.getTracks().forEach(t=>t.stop()); mediaStream = null; }
    if (audioCtx) { try { audioCtx.close(); } catch(_) {} audioCtx = null; }
    document.getElementById('micBtn')?.classList.remove('listening');
    isListening = false;
    setVoiceStatus('');
}

async function uploadForSTT(blob, lang){
    setVoiceStatus('⬆️ Sending to STT…');
    const form = new FormData();
    form.append('audio', blob, 'voice.webm');
    form.append('lang', lang);

    try{
        const res = await fetch('/api/stt', { method: 'POST', body: form });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const json = await res.json();
        const text = (json.text || '').trim();
        if (text){
            document.getElementById('chatInput').value = text;
            sendChat();
        } else {
            setVoiceStatus('No speech detected.');
        }
    }catch(err){
        setVoiceStatus('⚠️ STT failed: ' + err.message);
    }
}

/* -------- Text-to-Speech (Hindi/English) --------
   Use SpeechSynthesis first, else optional backend /api/tts → audio URL.
*/
function isWebSpeechTTSSupported(){ return 'speechSynthesis' in window; }

function speak(text, lang){
    if (!text) return;
    // Try browser TTS
    if (isWebSpeechTTSSupported()){
        const u = new SpeechSynthesisUtterance(text);
        u.lang = lang || getLang();   // 'hi-IN' will speak Hindi if a voice exists
        u.rate = 1.0; u.pitch = 1.0;
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(u);
        return;
    }
    // Backend fallback
    fetch('/api/tts?lang=' + encodeURIComponent(lang || getLang()), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text })
    }).then(r=>r.json()).then(j=>{
        if (j.url){
            const au = new Audio(j.url);
            au.play().catch(()=>{ /* ignore */ });
        }
    }).catch(()=>{ /* ignore */ });


}

    const SHEET_ID = "YOUR_SHEET_ID";
    const SHEET_NAME = "Sheet1"; // or gid=... form if you prefer
    const url = `https://docs.google.com/spreadsheets/d/1ccvrJetEMorEQBGLJaQo6pX9DnEcgDmoiZQ5UiE9MA8/edit?gid=0#gid=0`;

function getCookie(name){
    return document.cookie.split('; ').reduce((r, v) => {
        const parts = v.split('=');
        return parts[0] === name ? decodeURIComponent(parts.slice(1).join('=')) : r;
    }, '');
}

/**
 * Calls Spring Security logout.
 * Requirements:
 * - Security config uses CookieCsrfTokenRepository (so browser has XSRF-TOKEN cookie)
 * - logoutUrl("/auth/logout") (default POST)
 */


// Example usage after you fetched & parsed gviz JSON into `json`:



//loadSheetJSON();

// ---------- Action Notes: front-end ----------





