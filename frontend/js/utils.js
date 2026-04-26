const API = 'http://localhost:8080';

// ── Auth ─────────────────────────────────────────────────────────────────────
function getToken()      { return localStorage.getItem('ps_token'); }
function getEmail()      { return localStorage.getItem('ps_email') || ''; }
function getMerchantId() { return localStorage.getItem('ps_merchant_id') || ''; }
function isLoggedIn()    { return !!getToken(); }

function saveAuth(token, email, merchantId) {
  localStorage.setItem('ps_token', token);
  localStorage.setItem('ps_email', email);
  if (merchantId) localStorage.setItem('ps_merchant_id', merchantId);
}

function clearAuth() {
  localStorage.removeItem('ps_token');
  localStorage.removeItem('ps_email');
  localStorage.removeItem('ps_merchant_id');
  localStorage.removeItem('ps_fraud_badge');
}

function logout()      { clearAuth(); window.location.href = '../index.html'; }
function requireAuth() { if (!isLoggedIn()) window.location.href = '../index.html'; }

// ── API fetch ─────────────────────────────────────────────────────────────────
async function apiFetch(path, opts = {}) {
  try {
    const res = await fetch(`${API}${path}`, {
      ...opts,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
        'X-Merchant-Id': getMerchantId(),
        ...(opts.headers || {})
      }
    });

    if (res.status === 401) {
      clearAuth();
      window.location.href = '../index.html';
      return null;
    }

    if (!res.ok) {
      console.warn(`API ${path} returned ${res.status}`);
      return null;
    }

    return await res.json();
  } catch (e) {
    console.warn(`API ${path} failed:`, e.message);
    return null;
  }
}

// ── Formatters ────────────────────────────────────────────────────────────────
const fmt     = n  => '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 0 });
const fmtNum  = n  => Number(n).toLocaleString('en-IN');
const fmtDate = d  => new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
const scoreClass = s => { const n = parseFloat(s); return n >= 75 ? 'high' : n >= 50 ? 'medium' : 'low'; };

function statusPill(status, fraud) {
  if (fraud) return `<span class="pill flag"><span class="pill-dot"></span>FLAGGED</span>`;
  const m = {
    COMPLETED: 'ok', MATCHED: 'ok', SUCCESS: 'ok',
    FAILED: 'fail', REJECTED: 'fail', MISMATCH: 'fail',
    PENDING: 'warn', PROCESSING: 'warn', FRAUD_CHECK: 'warn',
    APPROVE: 'info', FLAG: 'warn', RUNNING: 'warn'
  };
  return `<span class="pill ${m[status] || 'info'}"><span class="pill-dot"></span>${status}</span>`;
}

// ── Fraud badge (persisted) ───────────────────────────────────────────────────
function getFraudBadge()       { return parseInt(localStorage.getItem('ps_fraud_badge') || '0'); }
function setFraudBadge(count)  { localStorage.setItem('ps_fraud_badge', count); }

// ── Sidebar ───────────────────────────────────────────────────────────────────
function sidebar(active) {
  const email = getEmail();
  const init  = email ? email[0].toUpperCase() : 'A';
  const badge = getFraudBadge();

  const links = [
    { id: 'overview',       icon: '◈', label: 'Overview',       href: 'overview.html' },
    { id: 'transactions',   icon: '↔', label: 'Transactions',   href: 'transactions.html' },
    { id: 'fraud',          icon: '⚠', label: 'Fraud Alerts',   href: 'fraud.html', badge: badge > 0 ? badge : null },
    { id: 'reconciliation', icon: '⊟', label: 'Reconciliation', href: 'reconciliation.html' },
    { id: 'reports',        icon: '◧', label: 'Reports',        href: 'reports.html' },
    { id: 'notifications',  icon: '🔔', label: 'Notifications', href: 'notifications.html' },
  ];

  return `
    <div class="sidebar-logo">
      <div class="logo-mark">🛡</div>
      <div class="logo-text">Pay<span>Shield</span></div>
    </div>
    <div class="nav-group-label">Menu</div>
    ${links.map(l => `
      <a class="nav-link ${active === l.id ? 'active' : ''}" href="${l.href}">
        <div class="nav-icon">${l.icon}</div>
        ${l.label}
        ${l.badge ? `<span class="nav-badge">${l.badge}</span>` : ''}
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

// ── Skeleton loader ───────────────────────────────────────────────────────────
function skeletonKpi() {
  return `<div class="skeleton" style="height:26px;width:80%;border-radius:4px;margin-bottom:6px"></div>
          <div class="skeleton" style="height:13px;width:55%;border-radius:4px"></div>`;
}

function showSkeletons(ids) {
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.innerHTML = skeletonKpi();
  });
}

// ── Mock data (fallback when API unavailable) ─────────────────────────────────
function mockDashboard() {
  return {
    totalVolume: 45230000, totalTransactions: 12445,
    successfulTransactions: 11698, failedTransactions: 747,
    fraudFlagged: 83, successRate: 94.0, avgTransactionValue: 3634,
    dailyVolumes: Array.from({ length: 14 }, (_, i) => ({
      date: new Date(Date.now() - (13 - i) * 86400000).toISOString().slice(0, 10),
      volume: Math.floor(Math.random() * 3000000 + 1500000),
      transactionCount: Math.floor(Math.random() * 400 + 600)
    }))
  };
}

function mockTransactions(n = 15) {
  const st = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'FAILED', 'FRAUD_CHECK'];
  const mt = ['CARD', 'UPI', 'NET_BANKING', 'WALLET', 'BANK_TRANSFER'];
  return Array.from({ length: n }, (_, i) => ({
    id: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
    amount: (Math.random() * 80000 + 500).toFixed(2),
    paymentMethod: mt[i % mt.length],
    customerEmail: `user${i + 1}@example.com`,
    fraudScore: (Math.random() * 100).toFixed(1),
    fraudFlagged: i === 2 || i === 7,
    status: st[i % st.length],
    initiatedAt: new Date(Date.now() - i * 3600000 * 1.5).toISOString()
  }));
}

function mockFraud(n = 10) {
  const rules = [['HIGH_AMOUNT'], ['VELOCITY_COUNT', 'NEW_DEVICE'], ['GEO_MISMATCH'], ['CARD_TESTING'], ['HIGH_AMOUNT', 'VELOCITY_COUNT']];
  return Array.from({ length: n }, (_, i) => ({
    transactionId: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
    score: (65 + Math.random() * 35).toFixed(1),
    decision: i < 2 ? 'REJECT' : i < 5 ? 'FLAG' : 'REVIEW',
    ruleTriggers: rules[i % rules.length],
    amount: (Math.random() * 80000 + 5000).toFixed(2),
    createdAt: new Date(Date.now() - i * 7200000).toISOString()
  }));
}

function mockNotifs() {
  const t = [
    { icon: '🚨', type: 'fraud',   cls: 'err',  title: 'High Fraud Score Detected',   msg: 'Transaction TXNABC123 scored 94.2 — auto-rejected. Rules: HIGH_AMOUNT, VELOCITY_COUNT' },
    { icon: '✅', type: 'payment', cls: 'ok',   title: 'Payment Completed',            msg: '₹12,500 from user@example.com processed via UPI successfully' },
    { icon: '❌', type: 'payment', cls: 'err',  title: 'Payment Failed',               msg: '₹8,200 failed — insufficient funds. Customer notified via email' },
    { icon: '⚠️', type: 'fraud',   cls: 'warn', title: 'Velocity Check Triggered',     msg: '5 transactions in 10 min from same IP. Manual review recommended' },
    { icon: '🔄', type: 'system',  cls: 'info', title: 'Reconciliation Completed',     msg: 'Daily run finished. 847/850 matched (99.6% match rate)' },
    { icon: '📧', type: 'system',  cls: 'info', title: 'Email Sent',                   msg: 'Payment receipt delivered to customer@example.com for order #45892' },
    { icon: '🛡', type: 'fraud',   cls: 'warn', title: 'New Device Detected',          msg: 'Unrecognized device for user2@example.com. Score: 72.4' },
    { icon: '💰', type: 'payment', cls: 'ok',   title: 'Large Payment Processed',      msg: '₹95,000 from merchant@corp.com completed via Net Banking' },
  ];
  return Array.from({ length: 20 }, (_, i) => {
    const x = t[i % t.length];
    return { id: i + 1, ...x, read: i > 5, createdAt: new Date(Date.now() - i * 3200000).toISOString() };
  });
}

// ── Chart defaults ────────────────────────────────────────────────────────────
const chartDefaults = {
  responsive: true,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: '#14161e', borderColor: '#23263a', borderWidth: 1,
      padding: 10, titleColor: '#9aa0bc', bodyColor: '#f0f2f8',
      titleFont: { size: 11 }, bodyFont: { size: 12 }
    }
  },
  scales: {
    x: { grid: { color: 'rgba(35,38,58,.6)' }, ticks: { color: '#5c6380', font: { size: 11 } } },
    y: { grid: { color: 'rgba(35,38,58,.6)' }, ticks: { color: '#5c6380', font: { size: 11 } } }
  }
};

// ── Toast ─────────────────────────────────────────────────────────────────────
function ensureToastContainer() {
  if (!document.getElementById('toast-container')) {
    const d = document.createElement('div');
    d.id = 'toast-container';
    document.body.appendChild(d);
  }
}

function toast(msg, type = 'info', duration = 3500) {
  ensureToastContainer();
  const icons = { success: '✓', error: '✕', info: 'ℹ', warn: '⚠' };
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span style="font-size:14px;flex-shrink:0">${icons[type]||'ℹ'}</span><span>${msg}</span>`;
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => {
    el.style.animation = 'slideOut .2s ease forwards';
    setTimeout(() => el.remove(), 200);
  }, duration);
}

// ── Countdown refresh ring ────────────────────────────────────────────────────
function createRefreshRing(containerId, intervalSec, onTick) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const r = 11, circ = 2 * Math.PI * r;
  container.innerHTML = `
    <div class="refresh-ring" title="Auto-refreshes every ${intervalSec}s — click to refresh now" onclick="onTick && onTick()">
      <svg width="28" height="28" viewBox="0 0 28 28">
        <circle cx="14" cy="14" r="${r}"/>
        <circle class="progress" id="ring-progress" cx="14" cy="14" r="${r}"
          stroke-dasharray="${circ}" stroke-dashoffset="0"/>
      </svg>
      <div class="icon">↻</div>
    </div>`;
  container.querySelector('.refresh-ring').onclick = onTick;

  let elapsed = 0;
  return setInterval(() => {
    elapsed++;
    const pct = elapsed / intervalSec;
    const offset = circ * (1 - pct);
    const prog = document.getElementById('ring-progress');
    if (prog) prog.style.strokeDashoffset = offset;
    if (elapsed >= intervalSec) {
      elapsed = 0;
      if (onTick) onTick();
    }
  }, 1000);
}

// ── Last updated ──────────────────────────────────────────────────────────────
function setLastUpdated(id) {
  const el = document.getElementById(id);
  if (el) el.textContent = 'Updated ' + new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

// ── Trend helper ──────────────────────────────────────────────────────────────
function trendBadge(current, previous) {
  if (!previous || previous === 0) return '';
  const pct = ((current - previous) / previous * 100).toFixed(1);
  const up = current >= previous;
  return `<span class="trend ${up ? 'up' : 'down'}">${up ? '▲' : '▼'} ${Math.abs(pct)}%</span>`;
}

// ── Mock data indicator ───────────────────────────────────────────────────────
function showMockBanner(containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  // Only show once per page
  if (el.querySelector('.mock-banner')) return;
  const banner = document.createElement('div');
  banner.className = 'mock-banner';
  banner.innerHTML = `<span class="mock-banner-icon">⚠</span>
    <span>Showing <strong>demo data</strong> — backend API unavailable or no data yet.</span>`;
  el.insertBefore(banner, el.firstChild);
}

function hideMockBanner(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.querySelector('.mock-banner')?.remove();
}
