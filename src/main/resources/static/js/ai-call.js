const API_BASE = "http://localhost:8082"; // prefix if your app is mounted under a sub-path
const API_CALL   = '/api/qcall/playground/call';

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

async function jsonFetch(url, opts) {
    const r = await fetch(url, opts);
    if (!r.ok) {
        const txt = await r.text().catch(() => "");
        throw new Error(`HTTP ${r.status}${txt ? `: ${txt}` : ""}`);
    }
    try { return await r.json(); } catch { return {}; }
}

function assistantIdOf(a) {
    // Adjust to your actual payload fields from /api/qcall/assistants
    return a.id || a._id || a.assistantId || a.knowledge_base_Id || a.name;
}

function renderAssistants(list) {
    const host = assistantsList();
    const empty = emptyState();
    const count = document.getElementById("countPill");
    const tmpl = document.getElementById("assistantCardTemplate");

    host.innerHTML = "";

    // Update count pill
    if (Array.isArray(list) && list.length) {
        count.textContent = list.length;
        count.style.display = "inline-block";
    } else {
        if (count) count.style.display = "none";
    }

    if (!Array.isArray(list) || list.length === 0) {
        empty.style.display = "block";
        return;
    }
    empty.style.display = "none";

    list.forEach((a) => {
        const id = assistantIdOf(a);
        const node = tmpl.content.firstElementChild.cloneNode(true);

        // dataset
        node.dataset.assistantId = id || "";

        // avatar / name / badge
        const avatar = node.querySelector('[data-role="avatar"]');
        avatar.src =
            a.avatar_url || a.image || a.avatarUrl || "https://placehold.co/112x112";

        const nameEl = node.querySelector('[data-role="name"]');
        nameEl.textContent = a.name || a.title || "Assistant";

        const dirEl = node.querySelector('[data-role="direction"]');
        dirEl.textContent = (a.direction || a.type || "OUTBOUND").toUpperCase();

        // title/use-case & company
        const titleEl = node.querySelector('[data-role="title"]');
        titleEl.textContent =
            a.title || a.use_case || a.description || "—";

        const companyEl = node.querySelector('[data-role="company"]');
        companyEl.textContent =
            a.company_name || a.company || a.org || "—";

        // hidden inputs for playgroundCall to find nm_<id> / ph_<id>
        const hName = node.querySelector('[data-role="hidden-name"]');
        const hPhone = node.querySelector('[data-role="hidden-phone"]');
        if (id) {
            hName.id = `nm_${id}`;
            hPhone.id = `ph_${id}`;
        }

        host.appendChild(node);
    });
}


function copyId(id) {
    navigator.clipboard?.writeText(id).then(
        () => alert("Assistant ID copied"),
        () => alert("Copy failed")
    );
}

async function loadAssistants() {
    statusText().innerHTML = `<span class="spinner"></span> Loading assistants…`;
    try {
        const assistants = await jsonFetch(`${API_BASE}/api/qcall/assistants`);
        renderAssistants(assistants || []);
        statusText().textContent = "Ready.";
    } catch (e) {
        console.error(e);
        statusText().textContent = "Failed to load assistants.";
    }
}

// Toggle create panel
function openCreate() {
    createStatus().textContent = "";
    caName().value = "";
    caCompany().value = "";
    caVoice().value = "";
    caPrompt().value = "";
    createCard().style.display = "block";
    window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
}
function cancelCreate() {
    createCard().style.display = "none";
}

async function createAssistant() {
    const name = (caName().value || "").trim();
    if (!name) {
        createStatus().textContent = "Please provide an assistant name.";
        return;
    }
    const company = (caCompany().value || "").trim();
    const voice = (caVoice().value || "").trim();
    const prompt = (caPrompt().value || "").trim();

    createStatus().innerHTML = `<span class="spinner"></span> Creating…`;

    try {
        // Payload mirrors what your controller expects; adjust keys if needed
        const payload = {
            name,
            company_name: company || undefined,
            voice_name: voice || undefined,
            system_prompt: prompt || undefined,
        };
        const resp = await jsonFetch(`${API_BASE}/api/qcall/assistants`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        createStatus().textContent = "Assistant created.";
        // Hide create panel and refresh list
        setTimeout(() => {
            cancelCreate();
            loadAssistants();
        }, 600);
    } catch (e) {
        createStatus().textContent = `Failed: ${e.message}`;
    }
}

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
        const r=await fetch(`${API_BASE}`+API_CALL,{
            method:'POST',
            headers:{'Content-Type':'application/json','Accept':'application/json'},
            body:JSON.stringify(payload)
        });
        const text=await r.text();
        if(!r.ok){
            toast(`Failed: HTTP ${r.status}`); console.error('Call error:',r.status,text);
        }else{
            toast('Call initiated ✔'); console.log('Call response:',text);
        }
    }catch(err){
        toast('Network error'); console.error(err);
    }finally{
        btn.disabled=false; btn.innerHTML=old;
    }
}

function toast(message) {
    // Create toast container if it doesn't exist
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

    // Create toast element
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

    // Auto remove after 3s
    setTimeout(() => {
        toastEl.style.opacity = "0";
        setTimeout(() => toastEl.remove(), 500);
    }, 3000);
}

/* wiring */
document.getElementById("refreshBtn").addEventListener("click", loadAssistants);
document.getElementById("openCreateBtn").addEventListener("click", openCreate);
document.getElementById("cancelCreateBtn").addEventListener("click", cancelCreate);
document.getElementById("createBtn").addEventListener("click", createAssistant);

/* init */
loadAssistants();


// Handle "Test" clicks on dynamic assistant cards
document.addEventListener("click", (e) => {
    const btn = e.target.closest('[data-action="assistant-call"]');
    if (!btn) return;

    const card = btn.closest(".assistant-card");
    const id = card?.dataset.assistantId;
    if (!id) {
        toast("Missing assistant id on card.");
        return;
    }

    // Make sure nm_<id> / ph_<id> exist and have values
    const hName = card.querySelector('[data-role="hidden-name"]');
    const hPhone = card.querySelector('[data-role="hidden-phone"]');

    // Prefer values user typed in your Quick Test fields if present
    const fallbackName = document.getElementById("nm_qt")?.value || "";
    let fallbackPhone = document.getElementById("ph_qt")?.value || "";

    // If no phone available, ask once
    if (!fallbackPhone) {
        fallbackPhone = window.prompt("Enter phone in E.164 format (e.g., +917350348860)") || "";
    }

    // Assign values + correct ids so playgroundCall can locate them
    if (hName) { hName.id = `nm_${id}`; hName.value = fallbackName; }
    if (hPhone) { hPhone.id = `ph_${id}`; hPhone.value = fallbackPhone; }

    // Go!
    playgroundCall(id, btn);
});

