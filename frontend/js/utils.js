const API = 'http://localhost:8080';
const MERCHANT_ID = '00000000-0000-0000-0000-000000000001';

function getToken()   { return localStorage.getItem('ps_token'); }
function getEmail()   { return localStorage.getItem('ps_email') || ''; }
function isLoggedIn() { return !!getToken(); }
function saveAuth(t, e) { localStorage.setItem('ps_token', t); localStorage.setItem('ps_email', e); }
function clearAuth()  { localStorage.removeItem('ps_token'); localStorage.removeItem('ps_email'); }
function logout()     { clearAuth(); window.location.href = '../index.html'; }
function requireAuth(){ if (!isLoggedIn()) window.location.href = '../index.html'; }

async function apiFetch(path, opts={}) {
  try {
    const res = await fetch(`${API}${path}`, { ...opts, headers: { 'Content-Type':'application/json', 'Authorization': `Bearer ${getToken()}`, ...(opts.headers||{}) }});
    if (!res.ok) throw new Error();
    return await res.json();
  } catch { return null; }
}

const fmt    = n => '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 0 });
const fmtNum = n => Number(n).toLocaleString('en-IN');
const fmtDate= d => new Date(d).toLocaleString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' });
const scoreClass = s => { const n = parseFloat(s); return n >= 75 ? 'high' : n >= 50 ? 'medium' : 'low'; };

function statusPill(status, fraud) {
  if (fraud) return `<span class="pill flag"><span class="pill-dot"></span>FLAGGED</span>`;
  const m = { COMPLETED:'ok', MATCHED:'ok', SUCCESS:'ok', FAILED:'fail', REJECTED:'fail', MISMATCH:'fail', PENDING:'warn', PROCESSING:'warn', FRAUD_CHECK:'warn', APPROVE:'info', FLAG:'warn', RUNNING:'warn' };
  return `<span class="pill ${m[status]||'info'}"><span class="pill-dot"></span>${status}</span>`;
}

function sidebar(active) {
  const email = getEmail(), init = email ? email[0].toUpperCase() : 'A';
  const links = [
    { id:'overview',       icon:'◈', label:'Overview',        href:'overview.html' },
    { id:'transactions',   icon:'↔', label:'Transactions',    href:'transactions.html' },
    { id:'fraud',          icon:'⚠', label:'Fraud Alerts',    href:'fraud.html', badge:'3' },
    { id:'reconciliation', icon:'⊟', label:'Reconciliation',  href:'reconciliation.html' },
    { id:'reports',        icon:'◧', label:'Reports',         href:'reports.html' },
    { id:'notifications',  icon:'🔔', label:'Notifications',  href:'notifications.html' },
  ];
  return `
    <div class="sidebar-logo">
      <div class="logo-mark">🛡</div>
      <div class="logo-text">Pay<span>Shield</span></div>
    </div>
    <div class="nav-group-label">Menu</div>
    ${links.map(l=>`
      <a class="nav-link ${active===l.id?'active':''}" href="${l.href}">
        <div class="nav-icon">${l.icon}</div>
        ${l.label}
        ${l.badge?`<span class="nav-badge">${l.badge}</span>`:''}
      </a>`).join('')}
    <div class="sidebar-bottom">
      <div class="user-row">
        <div class="user-av">${init}</div>
        <div class="user-meta">
          <div class="user-name">${email}</div>
          <div class="user-role">Admin</div>
        </div>
        <button class="btn-signout" onclick="logout()" title="Sign out">→</button>
      </div>
    </div>`;
}

function initPage(id) {
  requireAuth();
  const s = document.getElementById('sidebar');
  if (s) s.innerHTML = sidebar(id);
}

/* Mock data */
function mockDashboard() {
  return {
    totalVolume: 45230000, totalTransactions: 12445, successfulTransactions: 11698,
    failedTransactions: 747, fraudFlagged: 83, successRate: 94.0, avgTransactionValue: 3634,
    dailyVolumes: Array.from({length:14},(_,i)=>({
      date: new Date(Date.now()-(13-i)*86400000).toISOString().slice(0,10),
      volume: Math.floor(Math.random()*3000000+1500000),
      transactionCount: Math.floor(Math.random()*400+600)
    }))
  };
}

function mockTransactions(n=15) {
  const st = ['COMPLETED','COMPLETED','COMPLETED','FAILED','FRAUD_CHECK'];
  const mt = ['CARD','UPI','NET_BANKING','WALLET','BANK_TRANSFER'];
  return Array.from({length:n},(_,i)=>({
    id: 'TXN'+Math.random().toString(36).slice(2,10).toUpperCase(),
    amount: (Math.random()*80000+500).toFixed(2),
    paymentMethod: mt[i%mt.length], customerEmail: `user${i+1}@example.com`,
    fraudScore: (Math.random()*100).toFixed(1), fraudFlagged: i===2||i===7,
    status: st[i%st.length], initiatedAt: new Date(Date.now()-i*3600000*1.5).toISOString()
  }));
}

function mockFraud(n=10) {
  const rules = [['HIGH_AMOUNT'],['VELOCITY_COUNT','NEW_DEVICE'],['GEO_MISMATCH'],['CARD_TESTING'],['HIGH_AMOUNT','VELOCITY_COUNT']];
  return Array.from({length:n},(_,i)=>({
    transactionId: 'TXN'+Math.random().toString(36).slice(2,10).toUpperCase(),
    score: (65+Math.random()*35).toFixed(1), decision: i<2?'REJECT':i<5?'FLAG':'REVIEW',
    ruleTriggers: rules[i%rules.length], amount: (Math.random()*80000+5000).toFixed(2),
    createdAt: new Date(Date.now()-i*7200000).toISOString()
  }));
}

/* Chart defaults */
const chartDefaults = {
  responsive: true,
  plugins: {
    legend: { display: false },
    tooltip: { backgroundColor:'#14161e', borderColor:'#23263a', borderWidth:1, padding:10, titleColor:'#9aa0bc', bodyColor:'#f0f2f8', titleFont:{size:11}, bodyFont:{size:12} }
  },
  scales: {
    x: { grid:{color:'rgba(35,38,58,.6)'}, ticks:{color:'#5c6380',font:{size:11}} },
    y: { grid:{color:'rgba(35,38,58,.6)'}, ticks:{color:'#5c6380',font:{size:11}} }
  }
};
