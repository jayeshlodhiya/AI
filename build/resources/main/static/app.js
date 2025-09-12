/* ===== AI Retail Assistant – Frontend JS (fixed) =====
   How to point to your API:
   - Same origin (default): API_BASE = ""
   - Different host/port:   add <script>window.API_BASE="http://localhost:8080"</script> before this file
======================================================= */
//http://localhost:8080/
const API_BASE ="http://localhost:8080" //(window.API_BASE || "").replace(/\/+$/, ""); // trim trailing slash
const el = (q) => document.querySelector(q);
const chatBox = el('#chatBox');

// ---------- Utils ----------
const DEFAULT_TIMEOUT_MS = 15000;

async function fetchJSON(path, opts = {}, timeoutMs = DEFAULT_TIMEOUT_MS) {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(API_BASE + path, { ...opts, signal: controller.signal });
    const contentType = res.headers.get("content-type") || "";
    if (!res.ok) {
      const body = contentType.includes("application/json") ? await res.json().catch(() => ({})) : await res.text();
      const errMsg = typeof body === "string" ? body : JSON.stringify(body);
      throw new Error(`HTTP ${res.status} ${res.statusText}: ${errMsg}`);
    }
    return contentType.includes("application/json") ? res.json() : res.text();
  } finally {
    clearTimeout(id);
  }
}

function pushMsg(who, text) {
  const bubble = document.createElement('div');
  bubble.className = `rounded-2xl px-3 py-2 text-sm border ${who==='bot' ? 'bg-zinc-900 border-zinc-800 text-zinc-200 self-start' : 'bg-zinc-100 text-zinc-900 border-zinc-100 self-end'}`;
  bubble.textContent = text;
  const wrap = document.createElement('div');
  wrap.className = 'flex ' + (who==='bot' ? 'justify-start' : 'justify-end');
  wrap.appendChild(bubble);
  chatBox.appendChild(wrap);
  chatBox.scrollTop = chatBox.scrollHeight;
}

function setStatus(node, msg) {
  node.textContent = msg;
  node.classList.remove('text-zinc-400');
  node.classList.add('text-zinc-300');
}

// ---------- KPIs + Low stock ----------
async function loadLowStock() {
  const body = el('#lowStockBody');
  try {
    const data = await fetchJSON('/api/inventory/low-stock', { method: 'GET' });
    el('#kpiLowStock').textContent = data.length ?? 0;
    body.innerHTML = '';
    (data || []).forEach(row => {
      const tr = document.createElement('tr');
      tr.className = 'border-t border-zinc-800/60 hover:bg-zinc-900/60';
      tr.innerHTML = `
        <td class="px-3 py-2 font-medium">${row.sku ?? '-'}</td>
        <td>${row.variant ?? '-'}</td>
        <td>${row.qty ?? 0}</td>
        <td>${row.reorderLevel ?? '-'}</td>
        <td>${row.location ?? '-'}</td>
        <td class="text-right"><button class="btn btn-primary" data-sku="${row.sku}" data-variant="${row.variant}">Create PO</button></td>
      `;
      body.appendChild(tr);
    });
  } catch (e) {
    console.error(e);
    el('#kpiLowStock').textContent = '–';
    body.innerHTML = `<tr><td colspan="6" class="px-3 py-3 text-zinc-400">Failed to load low-stock. Check API_BASE and backend.</td></tr>`;
  }
}

// ---------- Demo chart ----------
function initSalesChart() {
  const ctx = document.getElementById('salesChart');
  if (!ctx) return;
  const labels = ['Jul 01','Jul 05','Jul 10','Jul 15','Jul 20','Jul 25','Jul 30'];
  const data = [24,18,31,27,36,42,48];
  new Chart(ctx, {
    type: 'line',
    data: { labels, datasets: [{ label: 'Sales (k)', data, borderWidth: 2, tension: 0.35 }] },
    options: {
      responsive: true,
      scales: {
        y: { grid: { color: '#27272a' }, ticks: { color: '#a1a1aa' } },
        x: { grid: { color: '#27272a' }, ticks: { color: '#a1a1aa' } }
      },
      plugins: { legend: { display: false } }
    }
  });
}

// ---------- Chat ----------
async function askChat(q) {
  pushMsg('user', q);
  try {
    const data = await fetchJSON('/api/chat/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenant_id: 'demo', q })
    });
    if (data && data.answer) {
      pushMsg('bot', String(data.answer));
    } else {
      pushMsg('bot', 'No answer returned. Check LLM config or RAG data.');
    }
    if (Array.isArray(data?.suggestions)) renderSuggestions(data.suggestions);
  } catch (e) {
    console.error(e);
    pushMsg('bot', `Chat failed: ${e.message}. Check API_BASE and backend logs.`);
  }
}

// ---------- Suggestions ----------
function renderSuggestions(items) {
  const ul = el('#suggList');
  ul.innerHTML = '';
  (items || []).forEach(s => {
    const li = document.createElement('li');
    li.className = 'flex items-center justify-between gap-2 border border-zinc-800 rounded-xl px-3 py-2';
    li.innerHTML = `
      <div>
        <div class="text-sm">${s.title || s.text || 'Suggestion'}</div>
        <div class="text-[11px] text-zinc-500">${s.type || ''} ${s.impact ? '· ' + s.impact : ''}</div>
      </div>
      <button class="btn btn-outline">Run</button>
    `;
    ul.appendChild(li);
  });
}

// ---------- Uploads ----------
function wireUpload(formSelector, type, msgSelector) {
  const form = el(formSelector);
  const msg = el(msgSelector);
  if (!form) return;
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    fd.append('type', type);
    try {
      const res = await fetch(API_BASE + '/api/ingest/file', { method: 'POST', body: fd });
      const contentType = res.headers.get('content-type') || '';
      const payload = contentType.includes('application/json') ? await res.json() : await res.text();
      setStatus(msg, typeof payload === 'string' ? payload : JSON.stringify(payload));
      if (type === 'inventory') loadLowStock();
    } catch (err) {
      console.error(err);
      setStatus(msg, `Upload failed: ${err.message}`);
    }
  });
}

// ---------- Boot ----------
document.addEventListener('DOMContentLoaded', () => {
  // Show which API base we’re using (handy for debugging)
  console.log('[UI] API_BASE =', API_BASE || '(same origin)');
  // Seed chat
  pushMsg('bot', 'Hi! Ask about inventory or policies. Try: "availability SKU-302 18in|22K|Gold".');

  // Wire chat
  const chatForm = document.getElementById('chatForm');
  const chatInput = document.getElementById('chatInput');
  chatForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    const q = chatInput.value.trim();
    if (!q) return;
    chatInput.value = '';
    askChat(q);
  });

  // Wire uploads
  wireUpload('#uploadInventory', 'inventory', '#uploadInventoryMsg');
  wireUpload('#uploadProducts', 'products', '#uploadProductsMsg');
  wireUpload('#uploadSales', 'sales', '#uploadSalesMsg');
  wireUpload('#uploadPolicy', 'rag_policies', '#uploadPolicyMsg');
  wireUpload('#uploadCatalog', 'rag_catalog', '#uploadCatalogMsg');

  // Init UI
  initSalesChart();
  loadLowStock();
});
window.__API_BASE__ = "https://ai-production-40f7.up.railway.app";