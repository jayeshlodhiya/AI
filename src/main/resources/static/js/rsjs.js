
    /* ---------- App Config ---------- */
    const TENANT_ID = 'demo';                       // set your real tenant
    window.currentTenantId = TENANT_ID;             // exposed for calls
    window.currentType = '';                        // optional coarse type
    window.currentDocType = '';                     // optional filter
    window.currentDocId = '';                       // optional doc filter
    const DEFAULT_TIMEOUT_MS = 15000;
    const API_BASE = (window.API_BASE || "").replace(/\/+$/, "");
    /* ---------- Demo Data ---------- */
    const leads = [
    { id:1, name: 'Rajesh Kumar', city:'Pune', budget: 95, status:'hot', score:92, phone:'+91 90000 00001' },
    { id:2, name: 'Priya Shah', city:'Pune', budget: 80, status:'warm', score:68, phone:'+91 90000 00002' },
    { id:3, name: 'Amit Verma', city:'Mumbai', budget: 120, status:'cold', score:40, phone:'+91 90000 00003' },
    { id:4, name: 'Neha Singh', city:'Bengaluru', budget: 75, status:'warm', score:71, phone:'+91 90000 00004' },
    { id:5, name: 'Suresh Patil', city:'Pune', budget: 60, status:'cold', score:43, phone:'+91 90000 00005' },
    { id:6, name: 'Aarav Mehta', city:'Pune', budget: 105, status:'hot', score:88, phone:'+91 90000 00006' },
    ];

    const properties = [
    { id:'S1-1103', city:'Pune', area:'Wakad', bhk:2, price:78, project:'Skyline Vista', tower:'A' },
    { id:'S1-1207', city:'Pune', area:'Hinjewadi', bhk:3, price:98, project:'Tech Meadows', tower:'C' },
    { id:'S2-0710', city:'Pune', area:'Baner', bhk:3, price:120, project:'Highstreet Park', tower:'B' },
    { id:'M1-0902', city:'Mumbai', area:'Powai', bhk:2, price:160, project:'Lakeview', tower:'E' },
    { id:'B1-1005', city:'Bengaluru', area:'Whitefield', bhk:2, price:82, project:'Green Acres', tower:'D' },
    { id:'B1-0604', city:'Bengaluru', area:'HSR', bhk:3, price:115, project:'Urban Nest', tower:'A' },
    { id:'P1-0408', city:'Pune', area:'Wakad', bhk:3, price:92, project:'Skyline Vista', tower:'B' },
    { id:'P1-0509', city:'Pune', area:'Pimple Saudagar', bhk:2, price:72, project:'North Square', tower:'A' },
    ];

    const competitors = [ /* ... same as your data ... */
    { id:1, project:'Lodha Belmondo', developer:'Lodha Group', area:'Wakad', bhk:2, price:85, perSqFt:8500, offers:'Free parking + 2% discount', competitionLevel:'high', amenities:['Swimming Pool','Gym','Garden','Security'], possession:'Dec 2024', location:'Near Wakad Bridge', strengths:'Premium brand, Good amenities', weaknesses:'Higher price, Delayed possession' },
    { id:2, project:'Godrej Emerald', developer:'Godrej Properties', area:'Hinjewadi', bhk:3, price:105, perSqFt:8200, offers:'No stamp duty + 1% discount', competitionLevel:'high', amenities:['Club House','Sports Complex','Kids Play Area'], possession:'Mar 2025', location:'IT Park Road', strengths:'Reputed developer, Location advantage', weaknesses:'Higher price, Traffic congestion' },
    { id:3, project:'Prestige Park Ridge', developer:'Prestige Group', area:'Baner', bhk:3, price:125, perSqFt:9000, offers:'Free modular kitchen + 3% discount', competitionLevel:'medium', amenities:['Rooftop Garden','Party Hall','Fitness Center'], possession:'Jun 2025', location:'Baner Hills', strengths:'Premium location, Good connectivity', weaknesses:'Steep price, Limited availability' },
    { id:4, project:'Kolte Patil Life Republic', developer:'Kolte Patil', area:'Pimple Saudagar', bhk:2, price:68, perSqFt:7200, offers:'Basic amenities free + 2% discount', competitionLevel:'low', amenities:['Basic Gym','Garden','Security'], possession:'Sep 2024', location:'Near Highway', strengths:'Affordable price, Early possession', weaknesses:'Basic amenities, Highway noise' },
    { id:5, project:'Tata Housing Vista', developer:'Tata Housing', area:'Wakad', bhk:2, price:78, perSqFt:7800, offers:'Free registration + 1.5% discount', competitionLevel:'medium', amenities:['Swimming Pool','Gym','Garden','Security'], possession:'Dec 2024', location:'Wakad Main Road', strengths:'Trusted brand, Good value', weaknesses:'Standard amenities, Average location' },
    { id:6, project:'Mahindra Lifespaces', developer:'Mahindra Group', area:'Hinjewadi', bhk:3, price:95, perSqFt:8000, offers:'Free interior + 2.5% discount', competitionLevel:'medium', amenities:['Club House','Sports Complex','Kids Play Area'], possession:'Mar 2025', location:'IT Park Extension', strengths:'Good amenities, Competitive pricing', weaknesses:'Delayed possession, Traffic issues' }
    ];

    let selectedLeadId = null;

    /* ---------- Helpers ---------- */
    function cap(s){ return s ? s.charAt(0).toUpperCase()+s.slice(1) : s; }
    function toggleSidebar(){ document.getElementById('sidebar').classList.toggle('open'); }
    function escapeHtml(s){ return (s||'').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m])); }
    function clip(text, maxLen = 160){ return text && text.length > maxLen ? text.slice(0, maxLen) + '…' : text; }
    function highlight(text, q, windowSize = 140){
    if (!text) return '';
    const i = text.toLowerCase().indexOf((q||'').toLowerCase());
    if (i === -1) return clip(text, 2*windowSize/1.5);
    const start = Math.max(0, i - Math.floor(windowSize/2));
    const end = Math.min(text.length, i + (q||'').length + Math.floor(windowSize/2));
    const pre = start > 0 ? '…' : '';
    const post = end < text.length ? '…' : '';
    const before = text.slice(start, i);
    const match = text.slice(i, i + (q||'').length);
    const after = text.slice(i + (q||'').length, end);
    return `${escapeHtml(pre + before)}<mark>${escapeHtml(match)}</mark>${escapeHtml(after + post)}`;
}

    /* ---------- RAG Search Calls ---------- */
    async function callRagSearch(query, tenantId, type, docType, docId, limit = 10) {
    const params = new URLSearchParams({ q: query, limit: String(limit) });
    if (tenantId) params.set('tenantId', tenantId);
    if (type) params.set('type', type);
    if (docType) params.set('docType', docType);
    if (docId) params.set('docId', docId);

    const data = await fetch('/api/chat/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenant_id: tenantId, q: query })
});

    //const res = await fetch(`/api/rag/search?${params.toString()}`);
    if (!data.ok) throw new Error(`Search failed: ${data.status}`);
    return data.json(); // { q, count, results:[{ id, tenantId, docId, type, docType, chunkIndex, content }] }
}
    async function fetchJSON(path, opts = {}, timeoutMs = DEFAULT_TIMEOUT_MS) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeoutMs);
    try {
    const res = await fetch(API_BASE + path, { ...opts, signal: controller.signal });
    const ct = res.headers.get("content-type") || "";
    if (!res.ok) {
    const body = ct.includes("application/json") ? await res.json().catch(()=>({})) : await res.text();
    throw new Error(typeof body === "string" ? body : JSON.stringify(body));
}
    return ct.includes("application/json") ? res.json() : res.text();
} finally { clearTimeout(id); }
}
    function renderRagResults(data, q) {
    // Safety
    if (!data) {
    return `<div><strong>No matches</strong> for “${escapeHtml(q || '')}”.</div>`;
}

    // --- If backend is returning the LLM-style rag_response ---
    if (data.type === 'rag_response' && (data.answer || data.reply)) {
    const raw = (data.answer || data.reply || '').trim();
    const bodyHtml = answerToHtml(raw);

    // Sources (compact, expandable)
    const sources = Array.isArray(data.sources) ? data.sources : [];
    const sourcesHtml = sources.length
    ? `<details style="margin-top:10px;">
           <summary style="cursor:pointer;font-weight:600;">Sources (${sources.length})</summary>
           <div style="margin-top:8px;">
             ${sources.slice(0, 5).map((s) => {
    const tag = [s.doc_type || s.type, s.doc_id].filter(Boolean).join(' • ') || 'source';
    const score = (typeof s.score === 'number') ? ` • score ${Number(s.score).toFixed(2)}` : '';
    const snippet = escapeHtml(String(s.text || '').slice(0, 300));
    return `<div style="padding:8px;border:1px solid var(--border);border-radius:8px;margin:6px 0;background:#f8fafc;">
                          <div style="font-size:12px;color:#64748b;margin-bottom:4px;">${escapeHtml(tag)}${score}</div>
                          <div style="font-size:13px;line-height:1.35;white-space:pre-wrap;">${snippet}${(s.text||'').length>300?'…':''}</div>
                        </div>`;
}).join('')}
           </div>
         </details>`
    : '';

    // Suggestions (chips)
    const suggHtml = Array.isArray(data.suggestions) && data.suggestions.length
    ? `<div style="margin-top:10px;">${
    data.suggestions.slice(0,6).map(s =>
    `<span class="chip" style="margin-right:6px;margin-top:6px;display:inline-block;">${escapeHtml(s.title || '')}</span>`
    ).join('')
}</div>`
    : '';

    return `
      <div style="display:grid;gap:8px;max-width:640px;">
        <div style="font-weight:700;">🔎 Answer</div>
        <div style="background:#fff;border:1px solid var(--border);border-radius:12px;padding:12px;box-shadow:var(--shadow);">
          <div style="font-size:14px;line-height:1.45;white-space:pre-wrap;">${bodyHtml}</div>
          ${sourcesHtml}
          ${suggHtml}
        </div>
      </div>
    `;
}

    // --- Fallback: old search-results shape (data.results + data.count) ---
    if (!data.results || !data.results.length) {
    return `<div><strong>No matches</strong> for “${escapeHtml(q || '')}”.</div>`;
}

    const items = data.results.map(r => {
    const snippet = highlight(r.content || r.snippet || '', q || '', 160);
    const metaParts = [];
    if (r.docId) metaParts.push(`📄 ${escapeHtml(r.docId)}`);
    if (r.docType) metaParts.push(`type: ${escapeHtml(r.docType)}`);
    if (r.chunkIndex !== undefined && r.chunkIndex !== null) metaParts.push(`chunk #${r.chunkIndex}`);
    return `<li style="margin-bottom:8px;">
      <div style="font-size:13px;line-height:1.45;">${snippet}</div>
      <div style="color:#6b7280;font-size:12px;margin-top:4px;">${metaParts.join(' • ')}</div>
    </li>`;
}).join('');

    return `
    <div>
      <div style="margin-bottom:6px;"><strong>Search results</strong> for “${escapeHtml(q || '')}” (${data.count ?? data.results.length})</div>
      <ol style="padding-left:18px;">${items}</ol>
      <div style="margin-top:6px;color:#6b7280;font-size:12px;">Tip: use <code>?your query</code> or toggle “Search docs”.</div>
    </div>
  `;
}

    /* ----- helpers ----- */

    // Turn the LLM's plain-text answer (with bullets/newlines) into HTML.
    function answerToHtml(raw) {
    const lines = String(raw || '').split('\n');
    let html = '';
    let inList = false;
    for (const line of lines) {
    const t = line.trim();
    if (!t) { if (inList) { html += '</ul>'; inList = false; } html += '<br/>'; continue; }
    if (/^[-*•]\s+/.test(t)) {
    if (!inList) { html += '<ul style="margin:8px 0 0 18px;">'; inList = true; }
    html += `<li>${escapeHtml(t.replace(/^[-*•]\s+/, ''))}</li>`;
} else if (/^\d+\.\s+/.test(t)) {
    if (!inList) { html += '<ol style="margin:8px 0 0 18px;">'; inList = true; }
    html += `<li>${escapeHtml(t.replace(/^\d+\.\s+/, ''))}</li>`;
} else {
    if (inList) { html += (html.endsWith('</ul>') || html.endsWith('</ol>')) ? '' : '</ul>'; inList = false; }
    html += `<div>${escapeHtml(t)}</div>`;
}
}
    if (inList && !html.endsWith('</ul>') && !html.endsWith('</ol>')) html += '</ul>';
    return html;
}


    /* ---------- Rendering: Leads ---------- */
    function renderLeads() {
    const tbody = document.querySelector('#leadTable tbody');
   // tbody.innerHTML = '';
    const sorted = [...leads].sort((a,b)=>b.score - a.score);
    sorted.forEach(l => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
          <td>${l.name}</td>
          <td>${l.city}</td>
          <td>₹${l.budget}L</td>
          <td><span class="badge ${l.status}">${cap(l.status)}</span></td>
          <td>${l.score}%</td>
          <td>
            <button class="btn" style="padding:6px 10px" onclick="selectLead(${l.id})">Select</button>
          </td>
        `;
    tbody.appendChild(tr);
});
    document.getElementById('hotCount').textContent = leads.filter(l=>l.status==='hot').length;
    document.getElementById('warmCount').textContent = leads.filter(l=>l.status==='warm').length;
    document.getElementById('coldCount').textContent = leads.filter(l=>l.status==='cold').length;

    const sel = document.getElementById('visitLead');
    sel.innerHTML='';
    leads.forEach(l=>{
    const opt = document.createElement('option');
    opt.value = l.id; opt.textContent = `${l.name} (${l.city})`;
    sel.appendChild(opt);
});
}

    function selectLead(id) {
    selectedLeadId = id;
    const lead = leads.find(l=>l.id===id);
    const s = document.getElementById('suggestions');
    s.innerHTML = '';
    const make = (html)=>{ const d=document.createElement('div'); d.style.marginBottom='10px'; d.innerHTML=html; return d; };
    s.appendChild(make(`<strong>${lead.name}</strong> • <span class="badge ${lead.status}">${cap(lead.status)}</span> • Budget <strong>₹${lead.budget}L</strong>`));
    s.appendChild(make(`💡 Offer suggestion: <strong>${lead.budget >= 90 ? 'Free parking' : 'Stamp duty rebate'}</strong>`));
    s.appendChild(make(`🗣️ Pitch angle: ${lead.city==='Pune' ? 'Highlight Hinjewadi IT connectivity' : 'Emphasize commute & schools'}`));
    s.appendChild(make(`📞 Next action: Follow-up call tomorrow 11:00 AM`));
}

    /* ---------- AI Assistant (Chat + RAG) ---------- */
    function appendMsg(role, htmlOrText, isHtml = false) {
    const chat = document.getElementById('chat');
    const div = document.createElement('div');
    div.className = `msg ${role}`;
    if (isHtml) { div.innerHTML = htmlOrText; } else { div.textContent = htmlOrText; }
    chat.appendChild(div);
    chat.scrollTop = chat.scrollHeight;
}

    async function sendChat() {
    const input = document.getElementById('chatInput');
    const text = (input.value || '').trim();
    appendMsg('user', text);
    //if(!text) return;
    //  const reply = aiReply(text);
    // appendMsg('ai', reply);
    // speak(reply, getLang());
    input.value='';
    const thinking = showThinking('Looking up');
    const searchToggle = document.getElementById('searchModeToggle');
    const explicitSearch = searchToggle && searchToggle.checked;
    const prefixSearch = text.startsWith('?');
    if (explicitSearch || prefixSearch) {
    const q = prefixSearch ? text.slice(1).trim() : text;
    try {
    const data = await callRagSearch(
    q,
    window.currentTenantId || '',
    window.currentType || '',
    window.currentDocType || '',
    window.currentDocId || '',
    10
    );
    //   console.log("Data : "+JSON.stringify(data));
    // alert("Data : "+data.toString());
    const html = renderRagResults(data, text);
    resolveThinking(thinking, html);
    // appendMsg('ai', html, true);
} catch (e) {
    //  appendMsg('ai', `Search error: ${e.message}`);
    removeThinking(thinking, `Error: ${err.message || 'failed to fetch'}`);
}
    return;
}

    // Fallback to demo property chat
    // setTimeout(()=> appendMsg('ai', aiReply(text)), 250);
}

    function aiReply(q) {
    const { bhk, maxPrice, cityLike } = parseQuery(q);
    const found = properties.filter(p =>
    (!bhk || p.bhk===bhk) && (!maxPrice || p.price<=maxPrice) && (!cityLike || (p.city+" "+p.area).toLowerCase().includes(cityLike))
    );
    if(found.length) {
    const list = found.slice(0,5).map(p=>`${p.project} • ${p.bhk}BHK • ₹${p.price}L • ${p.area}`).join('\n- ');
    return `I found ${found.length} matching units:\n- ${list}\nShall I draft a WhatsApp with the top 2?`;
}
    if(/draft|whatsapp|message/i.test(q)) {
    const lead = selectedLeadId ? leads.find(l=>l.id===selectedLeadId) : leads[0];
    return `Draft: Hi ${lead.name.split(' ')[0]}, sharing 2-3 options that match your budget. Site visits available this weekend. Reply with a suitable time.`;
}
    return 'You can ask things like: "3BHK under 1Cr in Hinjewadi" or type "?payment schedule" to search your docs.';
}

    function parseQuery(q) {
    const bhkMatch = q.match(/(\d)\s*bhk/i);
    const bhk = bhkMatch ? parseInt(bhkMatch[1]) : null;
    const priceMatch = q.match(/(?:₹\s*)?(\d+(?:\.\d+)?)\s*(cr|crore|crs|l|lac|lakh|lakhs)?/i);
    let maxPrice = null;
    if(priceMatch) {
    const num = parseFloat(priceMatch[1]);
    const unit = (priceMatch[2]||'l').toLowerCase();
    maxPrice = /cr|crore|crs/.test(unit) ? Math.round(num*100) : Math.round(num);
}
    const loc = q.match(/in\s+([a-z\s]+)/i);
    const cityLike = loc ? loc[1].trim().toLowerCase() : null;
    return { bhk, maxPrice, cityLike };
}

    /* ---------- Actions ---------- */
    function sendWhatsApp() {
    const lead = selectedLeadId ? leads.find(l=>l.id===selectedLeadId) : leads[0];
    const text = encodeURIComponent(`Hi ${lead.name.split(' ')[0]}, here are curated options based on your budget of ₹${lead.budget}L. Can I schedule a site visit?`);
    appendMsg('ai', `WhatsApp draft ready for ${lead.name}:\n${decodeURIComponent(text)}`);
}

    function generateAgreement() {
    const lead = selectedLeadId ? leads.find(l=>l.id===selectedLeadId) : leads[0];
    const content = `Booking Agreement (Demo)\n\nBuyer: ${lead.name}\nCity: ${lead.city}\nProvisional Amount: ₹${lead.budget} Lakhs\nDate: ${new Date().toLocaleString()}\n\nThis is a demo agreement generated by the AI Sales Dashboard.`;
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `Agreement_${lead.name.replace(/\s+/g,'_')}.txt`;
    document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
}

    function openVisitModal() { document.getElementById('visitModal').style.display='flex'; }
    function closeVisitModal() { document.getElementById('visitModal').style.display='none'; }
    function saveVisit() {
    const leadId = parseInt(document.getElementById('visitLead').value,10);
    const when = document.getElementById('visitDate').value;
    const notes = document.getElementById('visitNotes').value.trim();
    const lead = leads.find(l=>l.id===leadId);
    closeVisitModal();
    appendMsg('ai', `Site visit booked for ${lead.name} on ${new Date(when).toLocaleString()}${notes?`\nNotes: ${notes}`:''}`);
}

    function handleSearch() {
    const q = document.getElementById('aiSearch').value.trim();
    if(!q) return;
    appendMsg('user', `Search: ${q}`);
    appendMsg('ai', aiReply(q));
}
    function loadCallAgents() {
    const iframe = document.getElementById('callAgentsFrame');
    if (iframe && !iframe.src) {
    iframe.src = '/call-agents.html'; // same folder as rs.html
}
}

    // patch showSection to load on demand
    const _showSectionOrig = showSection;
    showSection = function(sectionName) {
    _showSectionOrig(sectionName);
    if (sectionName === 'callAgents') loadCallAgents();
    if(sectionName==='documentDetails') loadDocumentDetails();
};
    function loadPromotions() {

    const iframe = document.getElementById('promotionsFrame');
    if (iframe && !iframe.src) {
    // absolute path so it works regardless of current route
    iframe.src = '/promotions.html';
}
}
    function  loadImagePromotions(){
    const iframe = document.getElementById('imagePromotionsFrame');
    if (iframe && !iframe.src) {
    // absolute path so it works regardless of current route
    iframe.src = '/promotions.html';
}
}
    function loadDocumentDetails(){
    // alert("Documet");
    const iframe = document.getElementById('documentDetailsFrame');
    if (iframe && !iframe.src) {
    // absolute path so it works regardless of current route
    iframe.src = '/document-details.html';
}
}
    function loadChat() {
    const iframe = document.getElementById('chatFrame');
    if (iframe && !iframe.src) {
    // absolute path so it works regardless of current route
    iframe.src = '/chat.html';
}
}


    /* ---------- Section Management ---------- */
    function showSection(sectionName, ev) {
    document.querySelectorAll('.content-section').forEach(section => section.classList.remove('active'));
    document.querySelectorAll('.nav li').forEach(item => item.classList.remove('active'));
    const targetSection = document.getElementById(sectionName + 'Section');
    if (targetSection) targetSection.classList.add('active');
    if (ev && ev.target) ev.target.classList.add('active');

    if (sectionName === 'inventory')      loadInventory();
    else if (sectionName === 'analytics')  loadAnalytics();
    else if (sectionName === 'virtualTours') loadVirtualTours();
    else if (sectionName === 'competitors') loadCompetitors();
    else if (sectionName === 'documents')   loadDocuments();
    else if (sectionName === 'callAgents')  loadCallAgents();
    else if (sectionName === 'promotions')  loadPromotions();
    else if (sectionName === 'imagePromotions')  loadImagePromotions();
    else if (sectionName === 'chat')  loadChat();
    else if (sectionName === 'documentDetails')  loadDocumentDetails();
}

    /* ---------- Inventory ---------- */
    async function loadInventory() {
    try {
    const response = await fetch('/api/inventory/low-stock');
    if (response.ok) {
    const inventoryData = await response.json();
    renderInventory(inventoryData);
} else {
    renderInventory(getDemoInventoryData());
}
} catch (error) {
    renderInventory(getDemoInventoryData());
}
}
    function getDemoInventoryData() {
    return [
{ sku: 'SKU-001', product: 'Laptop', variant: '15" i7', qty: 5, reorderLevel: 10, location: 'Warehouse A', status: 'low' },
{ sku: 'SKU-002', product: 'Mouse', variant: 'Wireless', qty: 15, reorderLevel: 20, location: 'Warehouse B', status: 'medium' },
{ sku: 'SKU-003', product: 'Keyboard', variant: 'Mechanical', qty: 25, reorderLevel: 15, location: 'Warehouse A', status: 'in-stock' },
{ sku: 'SKU-004', product: 'Monitor', variant: '27" 4K', qty: 8, reorderLevel: 12, location: 'Warehouse C', status: 'low' },
{ sku: 'SKU-005', product: 'Headphones', variant: 'Noise Cancelling', qty: 30, reorderLevel: 25, location: 'Warehouse B', status: 'in-stock' }
    ];
}
    function renderInventory(data) {
    const tbody = document.querySelector('#inventoryTable tbody');
    tbody.innerHTML = '';
    let lowStockCount = 0, mediumStockCount = 0, inStockCount = 0;
    data.forEach(item => {
    const tr = document.createElement('tr');
    const status = getStockStatus(item.qty, item.reorderLevel);
    if (status === 'low') lowStockCount++;
    else if (status === 'medium') mediumStockCount++;
    else inStockCount++;
    tr.innerHTML = `
          <td>${item.sku}</td>
          <td>${item.product}</td>
          <td>${item.variant}</td>
          <td>${item.qty}</td>
          <td>${item.reorderLevel}</td>
          <td>${item.location}</td>
          <td><span class="badge ${status}">${cap(status)}</span></td>
        `;
    tbody.appendChild(tr);
});
    document.getElementById('lowStockCount').textContent = lowStockCount;
    document.getElementById('mediumStockCount').textContent = mediumStockCount;
    document.getElementById('inStockCount').textContent = inStockCount;
}
    function getStockStatus(qty, reorderLevel) {
    if (qty <= reorderLevel) return 'low';
    if (qty <= reorderLevel * 1.5) return 'medium';
    return 'in-stock';
}
    function refreshInventory() { loadInventory(); }
    function exportInventory() {
    const table = document.getElementById('inventoryTable');
    const rows = Array.from(table.querySelectorAll('tr'));
    let csv = 'SKU,Product,Variant,Quantity,Reorder Level,Location,Status\n';
    rows.slice(1).forEach(row => {
    const cells = Array.from(row.querySelectorAll('td'));
    csv += cells.map(cell => cell.textContent).join(',') + '\n';
});
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'inventory_export.csv';
    document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
}
    function addInventoryItem() { appendMsg('ai', 'Add inventory item functionality coming soon.'); }
    function searchInventory() {
    const searchTerm = document.getElementById('inventorySearch').value.toLowerCase();
    const rows = document.querySelectorAll('#inventoryTable tbody tr');
    rows.forEach(row => row.style.display = row.textContent.toLowerCase().includes(searchTerm) ? '' : 'none');
}

    /* ---------- Competitors ---------- */
    function loadCompetitors() { renderCompetitorsTable(competitors); updateCompetitionCounts(competitors); renderProjectComparison(); }
    function renderCompetitorsTable(data) {
    const tbody = document.querySelector('#competitorsTable tbody'); tbody.innerHTML = '';
    data.forEach(comp => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
          <td><strong>${comp.project}</strong></td>
          <td>${comp.developer}</td>
          <td>${comp.area}</td>
          <td>${comp.bhk}BHK</td>
          <td>₹${comp.price}L</td>
          <td>₹${comp.perSqFt.toLocaleString()}</td>
          <td style="max-width:200px;">${comp.offers}</td>
          <td><span class="badge ${comp.competitionLevel}">${cap(comp.competitionLevel)}</span></td>
          <td><button class="btn" style="padding:6px 10px" onclick="analyzeCompetitor(${comp.id})">Analyze</button></td>
        `;
    tbody.appendChild(tr);
});
}
    function updateCompetitionCounts(data) {
    document.getElementById('highCompCount').textContent = data.filter(c => c.competitionLevel === 'high').length;
    document.getElementById('mediumCompCount').textContent = data.filter(c => c.competitionLevel === 'medium').length;
    document.getElementById('lowCompCount').textContent = data.filter(c => c.competitionLevel === 'low').length;
}
    function filterCompetitors() {
    const areaFilter = document.getElementById('areaFilter').value;
    const filtered = areaFilter ? competitors.filter(c => c.area === areaFilter) : competitors;
    renderCompetitorsTable(filtered); updateCompetitionCounts(filtered);
}
    function analyzeCompetitor(compId) {
    const competitor = competitors.find(c => c.id === compId);
    if (!competitor) return;
    const analysisDiv = document.getElementById('competitiveAnalysis');
    analysisDiv.innerHTML = `
        <div style="background:#f8fafc; padding:16px; border-radius:8px; margin-bottom:16px;">
          <h3 style="margin:0 0 12px 0; color:var(--primary);">${competitor.project} - Analysis</h3>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:16px;">
            <div>
              <strong>Strengths:</strong>
              <ul style="margin:8px 0; padding-left:20px; color:#059669;">
                ${competitor.strengths.split(', ').map(s => `<li>${s}</li>`).join('')}
              </ul>
            </div>
            <div>
              <strong>Weaknesses:</strong>
              <ul style="margin:8px 0; padding-left:20px; color:#dc2626;">
                ${competitor.weaknesses.split(', ').map(w => `<li>${w}</li>`).join('')}
              </ul>
            </div>
          </div>
          <div style="margin-top:16px; padding:12px; background:#e0ebff; border-radius:8px;">
            <strong>💡 Competitive Advantage Opportunities:</strong>
            <ul style="margin:8px 0; padding-left:20px;">
              <li>Offer ${competitor.perSqFt > 8000 ? 'better pricing' : 'premium amenities'} to differentiate</li>
              <li>Highlight ${competitor.possession.includes('2025') ? 'earlier possession' : 'better location'} as key selling point</li>
              <li>Provide ${competitor.offers.includes('discount') ? 'additional value' : 'attractive discounts'} to attract buyers</li>
            </ul>
          </div>
        </div>
      `;
    renderProjectComparison(competitor);
}
    function renderProjectComparison(selectedCompetitor = null) {
    const comparisonDiv = document.getElementById('projectComparison');
    if (!selectedCompetitor) {
    comparisonDiv.innerHTML = `
          <div style="color:#6b7280; text-align:center; padding:20px;">
            <div style="font-size:24px; margin-bottom:8px;">🏆</div>
            <div>Select a competitor project to see detailed comparison</div>
            <div style="font-size:12px; margin-top:8px;">Compare pricing, amenities, and offers to create winning strategies</div>
          </div>
        `;
    return;
}
    const yourProjects = properties.filter(p => p.area === selectedCompetitor.area);
    if (!yourProjects.length) {
    comparisonDiv.innerHTML = `
          <div style="background:#fef3c7; padding:16px; border-radius:8px; text-align:center;">
            <strong>⚠️ No projects in ${selectedCompetitor.area}</strong>
            <div style="margin-top:8px; color:#6b7280;">Consider expanding to this area to compete with ${selectedCompetitor.developer}</div>
          </div>
        `;
    return;
}
    comparisonDiv.innerHTML = `
        <div style="background:#f8fafc; padding:16px; border-radius:8px;">
          <h3 style="margin:0 0 16px 0; color:var(--primary);">Competitive Analysis: ${selectedCompetitor.area}</h3>
          <div style="overflow:auto;">
            <table style="width:100%; border-collapse:collapse;">
              <thead>
                <tr style="background:#e2e8f0;">
                  <th style="padding:12px; text-align:left; border:1px solid var(--border);">Metric</th>
                  <th style="padding:12px; text-align:left; border:1px solid var(--border);">Competitor (${selectedCompetitor.project})</th>
                  <th style="padding:12px; text-align:left; border:1px solid var(--border);">Your Projects</th>
                  <th style="padding:12px; text-align:left; border:1px solid var(--border);">Recommendation</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="padding:12px; border:1px solid var(--border);"><strong>Price (2BHK)</strong></td>
                  <td style="padding:12px; border:1px solid var(--border);">₹${selectedCompetitor.price}L</td>
                  <td style="padding:12px; border:1px solid var(--border);">₹${yourProjects[0].price}L</td>
                  <td style="padding:12px; border:1px solid var(--border); color:${yourProjects[0].price < selectedCompetitor.price ? '#059669' : '#dc2626'};">
                    ${yourProjects[0].price < selectedCompetitor.price ? '✅ Price advantage' : '❌ Need better pricing'}
                  </td>
                </tr>
                <tr>
                  <td style="padding:12px; border:1px solid var(--border);"><strong>Per Sq.Ft</strong></td>
                  <td style="padding:12px; border:1px solid var(--border);">₹${selectedCompetitor.perSqFt.toLocaleString()}</td>
                  <td style="padding:12px; border:1px solid var(--border);">₹${Math.round((yourProjects[0].price || 0) * 100000 / 1200).toLocaleString()}</td>
                  <td style="padding:12px; border:1px solid var(--border); color:${Math.round((yourProjects[0].price || 0) * 100000 / 1200) < selectedCompetitor.perSqFt ? '#059669' : '#dc2626'};">
                    ${Math.round((yourProjects[0].price || 0) * 100000 / 1200) < selectedCompetitor.perSqFt ? '✅ Better value' : '❌ Need value proposition'}
                  </td>
                </tr>
                <tr>
                  <td style="padding:12px; border:1px solid var(--border);"><strong>Offers</strong></td>
                  <td style="padding:12px; border:1px solid var(--border);">${selectedCompetitor.offers}</td>
                  <td style="padding:12px; border:1px solid var(--border);">Standard offers</td>
                  <td style="padding:12px; border:1px solid var(--border); color:#f59e0b;">💡 Enhance offers to compete</td>
                </tr>
                <tr>
                  <td style="padding:12px; border:1px solid var(--border);"><strong>Possession</strong></td>
                  <td style="padding:12px; border:1px solid var(--border);">${selectedCompetitor.possession}</td>
                  <td style="padding:12px; border:1px solid var(--border);">Q2 2024</td>
                  <td style="padding:12px; border:1px solid var(--border); color:#059669;">✅ Earlier possession advantage</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      `;
}
    function generateCompetitiveOffer() {
    const selectedCompetitor = competitors.find(c => c.competitionLevel === 'high');
    if (!selectedCompetitor) return appendMsg('ai', 'No high-competition projects found.');
    const competitiveOffer = {
    title: `Beat ${selectedCompetitor.project} Offer!`,
    description: `Special competitive pricing to match or beat ${selectedCompetitor.developer}'s offers in ${selectedCompetitor.area}`,
    discount: Math.min(selectedCompetitor.perSqFt > 8000 ? 5 : 3, 8)
};
    addOfferToList(competitiveOffer);
    appendMsg('ai', `Competitive offer generated against ${selectedCompetitor.project}! Discount: ${competitiveOffer.discount}%.`);
}

    /* ---------- Offers ---------- */
    function createOffer() {
    const title = document.getElementById('offerTitle').value.trim();
    const description = document.getElementById('offerDescription').value.trim();
    const discount = document.getElementById('offerDiscount').value;
    const expiry = document.getElementById('offerExpiry').value;
    if (!title || !description || !discount || !expiry) return appendMsg('ai', 'Please fill in all fields to create an offer.');
    const offer = { id: Date.now(), title, description, discount: discount + '%', expiry, status:'active', createdAt: new Date().toLocaleDateString() };
    addOfferToList(offer);
    document.getElementById('offerTitle').value = '';
    document.getElementById('offerDescription').value = '';
    document.getElementById('offerDiscount').value = '';
    document.getElementById('offerExpiry').value = '';
    appendMsg('ai', `Offer "${title}" created successfully with ${discount}% discount!`);
}
    function addOfferToList(offer) {
    const offersList = document.getElementById('offersList');
    if (offersList.children.length === 1 && offersList.children[0].style.color === 'rgb(107, 114, 128)') offersList.innerHTML = '';
    const offerDiv = document.createElement('div');
    offerDiv.className = 'card'; offerDiv.style.marginBottom = '12px'; offerDiv.style.border = '1px solid var(--border)'; offerDiv.style.borderRadius = '8px'; offerDiv.style.padding = '12px';
    offerDiv.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
          <strong>${offer.title}</strong>
          <span class="badge ${offer.status === 'active' ? 'green' : 'cold'}">${cap(offer.status)}</span>
        </div>
        <div style="color:#6b7280; margin-bottom:8px;">${offer.description}</div>
        <div style="display:flex; gap:16px; font-size:12px; color:#64748b;">
          <span>Discount: ${offer.discount}</span>
          <span>Expires: ${offer.expiry}</span>
          <span>Created: ${offer.createdAt}</span>
        </div>`;
    offersList.appendChild(offerDiv);
}

    /* ---------- Analytics ---------- */
    function loadAnalytics() {
    const totalLeads = leads.length;
    const hotLeads = leads.filter(l => l.status === 'hot').length;
    const totalBudget = leads.reduce((sum, l) => sum + l.budget, 0);
    const conversionRate = Math.round((hotLeads / totalLeads) * 100);
    document.getElementById('totalLeads').textContent = totalLeads;
    document.getElementById('totalSales').textContent = `₹${totalBudget}L`;
    document.getElementById('conversionRate').textContent = `${conversionRate}%`;
}

    /* ---------- Virtual Tours ---------- */
    function loadVirtualTours() {
    const recentTours = JSON.parse(localStorage.getItem('recentTours') || '[]');
    renderRecentTours(recentTours);
}
    function scheduleVirtualTour() {
    const property = document.getElementById('tourProperty').value;
    if (!property) return appendMsg('ai', 'Please select a property for the virtual tour.');
    const tour = { id: Date.now(), property, scheduledAt: new Date().toLocaleString(), status:'scheduled' };
    const recentTours = JSON.parse(localStorage.getItem('recentTours') || '[]');
    recentTours.unshift(tour); if (recentTours.length > 5) recentTours.pop();
    localStorage.setItem('recentTours', JSON.stringify(recentTours));
    renderRecentTours(recentTours);
    document.getElementById('tourProperty').value = '';
    appendMsg('ai', `Virtual tour scheduled for ${property}!`);
}
    function renderRecentTours(tours) {
    const recentToursDiv = document.getElementById('recentTours');
    if (!tours.length) return recentToursDiv.innerHTML = '<div style="color:#6b7280">No recent virtual tours scheduled.</div>';
    recentToursDiv.innerHTML = tours.map(tour => `
        <div style="padding:8px 0; border-bottom:1px solid var(--border);">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <div>
              <strong>${tour.property}</strong>
              <div style="font-size:12px; color:#6b7280;">${tour.scheduledAt}</div>
            </div>
            <span class="badge ${tour.status === 'scheduled' ? 'green' : 'cold'}">${cap(tour.status)}</span>
          </div>
        </div>`).join('');
}

    /* ---------- Documents (Upload + Stats + List + Search) ---------- */
    let selectedFiles = [];
    const SUPPORTED_EXTS = ['pdf','png','jpg','jpeg','gif','bmp','tiff','doc','docx','xls','xlsx','txt'];
    function extOf(name){ return (name.split('.').pop() || '').toLowerCase(); }

    function handleFileSelect(event) {
    const files = Array.from(event.target.files);
    const ok = [];
    const skipped = [];

    files.forEach(f => {
    const ext = extOf(f.name);
    if (!SUPPORTED_EXTS.includes(ext)) return skipped.push(`${f.name} (unsupported)`);
    if (tooBig(f)) return skipped.push(`${f.name} (> ${MAX_MB} MB)`);
    ok.push(f);
});

    if (skipped.length) {
    appendMsg('ai', `Some files were skipped:\n- ${skipped.join('\n- ')}`);
}

    selectedFiles = ok;
    renderSelectedFiles();
}

    function handleDrop(e){
    e.preventDefault(); e.currentTarget.classList.remove('dragover');
    const files = Array.from(e.dataTransfer.files);
    const ok = [];
    const skipped = [];

    files.forEach(f => {
    const ext = extOf(f.name);
    if (!SUPPORTED_EXTS.includes(ext)) return skipped.push(`${f.name} (unsupported)`);
    if (tooBig(f)) return skipped.push(`${f.name} (> ${MAX_MB} MB)`);
    ok.push(f);
});

    if (skipped.length) appendMsg('ai', `Some files were skipped:\n- ${skipped.join('\n- ')}`);
    handleFileSelect({ target: { files: ok } });
}

    function handleDragOver(e){ e.preventDefault(); e.currentTarget.classList.add('dragover'); }
    function handleDragLeave(e){ e.currentTarget.classList.remove('dragover'); }

    function renderSelectedFiles(){
    const show = selectedFiles.length > 0;
    document.getElementById('selectedFiles').style.display = show ? 'block' : 'none';
    document.getElementById('uploadBtn').style.display = show ? 'inline-flex' : 'none';
    const fileList = document.getElementById('fileList');
    fileList.innerHTML = '';
    selectedFiles.forEach((file, i) => {
    const row = document.createElement('div');
    row.style.cssText = 'display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border);';
    row.innerHTML = `
          <div>
            <strong>${file.name}</strong>
            <div style="font-size:12px;color:#6b7280;">${(file.size/1024/1024).toFixed(2)} MB</div>
          </div>
          <button class="btn" style="padding:4px 8px;font-size:12px;" onclick="removeFile(${i})">Remove</button>`;
    fileList.appendChild(row);
});
}
    function removeFile(i){ selectedFiles.splice(i,1); renderSelectedFiles(); }

    function openUploadModal(){ document.getElementById('uploadModal').style.display='flex'; updateProgress('Initializing...', 0); }
    function closeUploadModal(){ document.getElementById('uploadModal').style.display='none'; }
    function updateProgress(text, pct){ document.getElementById('progressText').textContent = text; document.getElementById('progressBar').style.width = `${pct}%`; }

    async function processAndUploadFile(file, docType) {
    const form = new FormData();
    form.append('file', file);
    form.append('type', `rag_${docType}`);
    form.append('tenant_id', TENANT_ID);
    const res = await fetch('/api/ingest/file', { method: 'POST', body: form });
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
    return await res.json(); // { ok:true, docId:"...", chunks: N }
}

    async function processAndUploadFileImg(file, docType) {
    let urlPath ;
    const form = new FormData();
    const blob = await compressImage(file);


    form.append('type', `rag_${docType}`);
    form.append('tenant_id', TENANT_ID);

    if(file.type==="image/jpg"){
    urlPath = "/api/ingest/scan";
    form.append('file', new File([blob], name, { type: 'image/jpeg' }));
}else{
    form.append('file', file);
    urlPath = "/api/ingest/file";
}
    console.log("File info",file.type);
    console.log("Url path",urlPath);
    const res = await fetch(urlPath, { method: 'POST', body: form });
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
    return await res.json(); // { ok:true, docId:"...", chunks: N }
}

    async function uploadDocuments(){
    if (!selectedFiles.length) return appendMsg('ai','Please select files to upload.');
    const docType = document.getElementById('docType').value;
    openUploadModal();
    let done = 0, totalChunks = 0;
    for (let i=0; i<selectedFiles.length; i++){
    const f = selectedFiles[i];
    let result
    updateProgress(`Uploading ${f.name}…`, (i/selectedFiles.length)*100);
    try{
    if(f.type==="image/jpg"){
    result = await processAndUploadFileImg(f, docType);
}else{
    result = await processAndUploadFile(f, docType);
}

    done++; totalChunks += result.chunks || 0;
    updateProgress(`Processed ${f.name} (${result.chunks||0} chunks)`, ((i+1)/selectedFiles.length)*100);
    await new Promise(r=>setTimeout(r, 300));
}catch(err){
    console.error(err); appendMsg('ai', `❌ ${f.name}: ${err.message}`);
}
}
    updateProgress('Upload complete!', 100);
    setTimeout(()=>{
    closeUploadModal();
    selectedFiles = []; document.getElementById('fileUpload').value = ''; renderSelectedFiles();
    loadDocuments(); // refresh list & stats
    appendMsg('ai', `✅ Processed ${done} document(s), created ${totalChunks} RAG chunk(s).`);
    appendMsg('ai',`Checking for actionable points...`);
    fetchActionItems('12'); // <-- triggers extraction and shows sticky notes

}, 800);
}

    async function loadDocumentStats(){
    try{
    const r = await fetch(`/api/rag/stats?tenant_id=${TENANT_ID}`, { cache:'no-store' });
    if(!r.ok) throw new Error('stats http '+r.status);
    const s = await r.json(); // { totalDocuments, totalChunks, searchableContent }
    setStatsUI(s.totalDocuments, s.totalChunks, s.searchableContent);
}catch(_){
    setStatsUI(12, 156, '2.3 MB'); // demo fallback
}
}
    function setStatsUI(totalDocs, chunks, size){
    document.querySelector('#documentStats div:nth-child(1) div:first-child').textContent = totalDocs;
    document.querySelector('#documentStats div:nth-child(2) div:first-child').textContent = chunks;
    document.querySelector('#documentStats div:nth-child(3) div:first-child').textContent = size;
}

    async function loadDocumentList(){
    const list = document.getElementById('documentList');
    try{
    const r = await fetch(`/api/rag/docs?tenant_id=${TENANT_ID}`, { cache:'no-store' });
    if(!r.ok) throw new Error('docs http '+r.status);
    const docs = await r.json(); // [{docId,name,type,size,chunks,uploaded}]
    renderDocList(docs);
}catch(_){
    renderDocList([
{ docId:'policy-1', name:'Company Policy.pdf', type:'policy', size:'2.1 MB', chunks:15, uploaded:'2024-01-15' },
{ docId:'catalog-1', name:'Product Catalog.pdf', type:'catalog', size:'1.8 MB', chunks:23, uploaded:'2024-01-14' },
{ docId:'brochure-1', name:'Project Brochure.docx', type:'brochure', size:'3.2 MB', chunks:18, uploaded:'2024-01-13' },
{ docId:'manual-1', name:'User Manual.doc', type:'manual', size:'4.5 MB', chunks:31, uploaded:'2024-01-12' }
    ]);
}
}

    function renderDocList(docs){
    const list = document.getElementById('documentList');
    if(!docs || !docs.length){
    list.innerHTML = `
          <div style="color:#6b7280; text-align:center; padding:20px;">
            <div style="font-size:24px; margin-bottom:8px;">📚</div>
            <div>No documents uploaded yet</div>
            <div style="font-size:12px; margin-top:8px;">Upload your first document to get started</div>
          </div>`;
    return;
}
    list.innerHTML = docs.map(d => `
        <div style="display:flex; justify-content:space-between; align-items:center; padding:12px; border:1px solid var(--border); border-radius:8px; margin-bottom:8px;">
          <div style="display:flex; align-items:center; gap:12px;">
            <div style="font-size:24px;">${iconFor(d.name)}</div>
            <div>
              <div style="font-weight:600;">${d.name}</div>
              <div style="font-size:12px; color:#6b7280;">
                ${cap(d.type)} • ${d.size || '-'} • ${d.chunks||0} chunks • ${d.uploaded || ''}
              </div>
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn" style="padding:6px 10px; font-size:12px;" onclick="searchInDocument('${(d.docId||d.name).replace(/'/g, "\\'")}')">Search</button>
            <button class="btn" style="padding:6px 10px; font-size:12px;" onclick="viewDocument('${(d.docId||d.name).replace(/'/g, "\\'")}')">View</button>
            <button class="btn" style="padding:6px 10px; font-size:12px;background:#fee2e2;color:#b91c1c" onclick="deleteDocument('${(d.docId||d.name).replace(/'/g, "\\'")}')">Delete</button>
          </div>
        </div>`).join('');
}

    function iconFor(name){
    const e = extOf(name);
    if (e==='pdf') return '📄';
    if (e==='doc' || e==='docx') return '📝';
    if (e==='xls' || e==='xlsx') return '📊';
    if (['png','jpg','jpeg','gif','bmp','tiff','webp'].includes(e)) return '🖼️';
    return '🖼️';
}

    async function deleteDocument(docId){
    if(!confirm('Delete this document and its RAG chunks?')) return;
    try{
    const r = await fetch(`/api/rag/docs/${encodeURIComponent(docId)}?tenant_id=${TENANT_ID}`, { method:'DELETE' });
    if(!r.ok) throw new Error('HTTP '+r.status);
    appendMsg('ai', `🗑️ Deleted ${docId}`);
    loadDocuments();
}catch(err){
    appendMsg('ai', `❌ Delete failed: ${err.message}`);
}
}

    async function searchInDocument(docId){
    const q = prompt(`Search in ${docId}:`);
    if(!q) return;
    appendMsg('user', `Search "${q}" in ${docId}`);
    try{
    const data = await callRagSearch(q, TENANT_ID, '', '', docId, 10);
    appendMsg('ai', renderRagResults(data, q), true);
}catch(e){
    appendMsg('ai', `Search error: ${e.message}`);
}
}

    function viewDocument(docId){ appendMsg('ai', `Opening viewer for ${docId} (demo).`); }

    async function searchDocuments() {
    const query = document.getElementById('docSearch').value.trim();
    if (!query) return loadDocumentList();
    appendMsg('user', `Searching documents for: "${query}"`);
    try{
    const data = await callRagSearch(query, TENANT_ID, '', '', '', 10);
    appendMsg('ai', renderRagResults(data, query), true);
}catch(e){
    appendMsg('ai', `Search error: ${e.message}`);
}
}

    function loadDocuments(){ loadDocumentStats(); loadDocumentList(); }

    /* ---------- Init & Shortcuts ---------- */
    renderLeads(); selectLead(1);
    document.getElementById('chatInput').addEventListener('keydown', e=>{ if(e.key==='Enter' && !e.shiftKey) { e.preventDefault(); sendChat(); }});
    document.getElementById('aiSearch').addEventListener('keydown', e=>{ if(e.key==='Enter') handleSearch(); });
    document.getElementById('inventorySearch').addEventListener('keydown', e=>{ if(e.key==='Enter') searchInventory(); });
    document.getElementById('areaFilter').addEventListener('change', filterCompetitors);
    document.getElementById('docSearch').addEventListener('keydown', e=>{ if(e.key==='Enter') searchDocuments(); });

    // Show a "thinking..." message and return a handle you can later replace
    function showThinking(text = 'Thinking') {
    const chat = document.getElementById('chat');
    const div = document.createElement('div');
    div.className = 'msg ai';
    div.innerHTML = `
    <span class="thinking">
      <span class="spinner" aria-hidden="true"></span>
      <span class="think-text">${escapeHtml(text)}</span>
      <span class="dots"></span>
    </span>
  `;
    chat.appendChild(div);
    chat.scrollTop = chat.scrollHeight;

    // animate dots: "", ".", "..", "..."
    let dots = 0;
    const interval = setInterval(() => {
    dots = (dots + 1) % 4;
    const el = div.querySelector('.dots');
    if (el) el.textContent = '.'.repeat(dots);
}, 350);

    return { div, interval };
}

    // Replace the thinking bubble with final HTML
    function resolveThinking(handle, finalHtml) {
    if (!handle) return;
    clearInterval(handle.interval);
    handle.div.innerHTML = finalHtml;   // finalHtml is trusted output from your renderer
}

    // If you need to cancel it (on error etc.)
    function removeThinking(handle, fallbackText) {
    if (!handle) return;
    clearInterval(handle.interval);
    if (fallbackText) {
    handle.div.textContent = fallbackText;
} else {
    handle.div.remove();
}
}
    async function handleSearch() {
    const q = document.getElementById('aiSearch').value.trim();
    if (!q) return;

    appendMsg('user', `Search: ${q}`);
    const thinking = showThinking('Searching');

    try {
    const res = await fetch(`/api/rag/search?` + new URLSearchParams({
    q, tenantId: 'demo', limit: 8
}), { cache: 'no-store' });
    const payload = await res.json();
    const html = renderRagResults(payload, q);
    resolveThinking(thinking, html);
} catch (e) {
    removeThinking(thinking, `Search error: ${e.message || e}`);
}
}
    function fileIcon(name){
    const e = extOf(name);
    if (['png','jpg','jpeg','gif','bmp','tiff','webp'].includes(e)) return '🖼️';
    if (e==='pdf') return '📄';
    if (e==='doc' || e==='docx') return '📝';
    if (e==='xls' || e==='xlsx') return '📊';
    return '📁';
}
    const MAX_MB = 25;
    function tooBig(f){ return (f.size/1024/1024) > MAX_MB; }

    function renderSelectedFiles(){
    const show = selectedFiles.length > 0;
    document.getElementById('selectedFiles').style.display = show ? 'block' : 'none';
    document.getElementById('uploadBtn').style.display = show ? 'inline-flex' : 'none';

    const fileList = document.getElementById('fileList');
    fileList.innerHTML = '';

    selectedFiles.forEach((file, i) => {
    const row = document.createElement('div');
    row.style.cssText = 'display:flex;gap:12px;align-items:center;padding:8px 0;border-bottom:1px solid var(--border);';

    const isImg = /^image\//.test(file.type) || ['png','jpg','jpeg','gif','bmp','tiff','webp'].includes(extOf(file.name));
    const thumb = document.createElement(isImg ? 'img' : 'div');
    thumb.style.cssText = 'width:44px;height:44px;flex:0 0 44px;border-radius:6px;object-fit:cover;background:#f1f5f9;display:flex;align-items:center;justify-content:center;font-size:20px;';
    if (isImg) {
    const reader = new FileReader();
    reader.onload = e => { thumb.src = e.target.result; };
    reader.readAsDataURL(file);
} else {
    thumb.textContent = fileIcon(file.name);
}

    const meta = document.createElement('div');
    meta.style.cssText = 'flex:1;min-width:0;';
    meta.innerHTML = `
      <div style="font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(file.name)}</div>
      <div style="font-size:12px;color:#6b7280;">
        ${(file.size/1024/1024).toFixed(2)} MB • ${file.type || ('.'+extOf(file.name))}
      </div>
    `;

    const rm = document.createElement('button');
    rm.className = 'btn';
    rm.style.cssText = 'padding:4px 8px;font-size:12px;';
    rm.textContent = 'Remove';
    rm.onclick = () => removeFile(i);

    row.appendChild(thumb);
    row.appendChild(meta);
    row.appendChild(rm);
    fileList.appendChild(row);
});
}



    const ACTIONS_ENDPOINT = '/api/actions/extract';
    const PERFORM_ENDPOINT = '/api/actions/perform';
    //const TENANT_ID = 'demo'; // keep consistent with your app
    async function fetchActionItems(docId) {
    try {
    // POST so we can send longer context if needed later
    const r = await fetch(ACTIONS_ENDPOINT, {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ tenantId: TENANT_ID, docId })
});
    if (!r.ok) throw new Error('actions http '+r.status);
    const data = await r.json();
    if (Array.isArray(data.items) && data.items.length) {
    renderActionNotes(data.items);
}
} catch (e) {
    console.warn('Action extract failed', e);
}
}

    function renderActionNotes(items) {
    const dock = document.getElementById('actionNotesDock');
    const bell = document.getElementById('actionBell');
    dock.innerHTML = items.map(renderNote).join('');
    dock.style.display = 'block';
    bell.style.display = 'none';
}

    function renderNote(item) {
    const id = escapeHtml(item.id || crypto.randomUUID());
    const title = escapeHtml(item.title || 'Suggested Action');
    const desc = escapeHtml(item.description || '');
    const type = escapeHtml(item.type || 'task');
    const conf = typeof item.confidence === 'number' ? Math.round(item.confidence*100) : null;
    const source = item.source?.docName ? `• ${escapeHtml(item.source.docName)}` : '';
    const meta = [type, conf!=null?`confidence ${conf}%`:null, source].filter(Boolean).join(' · ');

    return `
    <div class="note" id="note-${id}">
      <span class="tag">${type.toUpperCase()}</span>
      <div class="title">${title}</div>
      <div class="meta">${meta}</div>
      <div style="margin-top:6px;">${desc}</div>
      ${item.parameters ? `<div class="meta" style="margin-top:6px;">Params: <code>${escapeHtml(JSON.stringify(item.parameters))}</code></div>`:''}
      <div class="btns">
        <button class="btn-primary" onclick='confirmPerform(${JSON.stringify(item)})'>Do it</button>
        <button class="btn-ghost" onclick='dismissNote("${id}")'>Dismiss</button>
      </div>
    </div>
  `;
}

    function dismissNote(id) {
    const el = document.getElementById('note-'+id);
    if (el) el.remove();
    const dock = document.getElementById('actionNotesDock');
    const bell = document.getElementById('actionBell');
    if (!dock.children.length) { dock.style.display='none'; bell.style.display='inline-flex'; }
}

    function confirmPerform(item) {
    // 2-step consent
    if (!confirm(`Proceed with: ${item.title}?`)) return;
    performAction(item);
}

    async function performAction(item) {
    try {
    const r = await fetch(PERFORM_ENDPOINT, {
    method:'POST',
    headers:{'Content-Type':'application/json'},
    body: JSON.stringify({
    tenantId: TENANT_ID,
    action: item
})
});
    const data = await r.json().catch(()=>({}));
    const ok = r.ok && (data.ok !== false);
    const id = item.id || '';
    const el = document.getElementById('note-'+id);
    if (ok) {
    if (el) el.innerHTML += `<div class="done">✅ Done</div>`;
    appendMsg('ai', `Action executed: ${item.title}`);
} else {
    appendMsg('ai', `Action failed: ${data.error || r.statusText}`);
}
} catch (e) {
    appendMsg('ai', `Action error: ${e.message}`);
}
}

    function escapeHtml(s=''){ return s.replace(/[&<>"'`=\/]/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[c])); }
    function toggleActionDock(open) {
    const dock = document.getElementById('actionNotesDock');
    const bell = document.getElementById('actionBell');
    const willOpen = open ?? (dock.style.display === 'none');
    dock.style.display = willOpen ? 'block' : 'none';
    bell.style.display = 'none';
}
    const DEFAULT_API_BASE = window.location.origin;
    async function doLogout() {
    try {
    const url  = `${DEFAULT_API_BASE}/auth/logout`;
    const res = await fetch(url, {
    method: 'POST',
    credentials: 'include'
});
    if (res.ok) {
    // force HTTPS, same origin
    window.location.assign(`${window.location.origin}/?logout`);
} else {
    window.location.assign(`${window.location.origin}/?logout`);
    // alert('Logout failed: ' + res.status);
}
} catch (e) {
    window.location.assign(`${window.location.origin}/?logout`);
}
}
    window.__API_BASE__ = "https://ai-production-40f7.up.railway.app";