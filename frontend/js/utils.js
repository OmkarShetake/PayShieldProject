const API = 'http://localhost:8080';

// ── Theme Management ─────────────────────────────────────────────────────────
function getTheme() { return localStorage.getItem('ps_theme') || 'dark'; }
function setTheme(theme) {
  localStorage.setItem('ps_theme', theme);
  document.documentElement.setAttribute('data-theme', theme);
  broadcastThemeChange(theme);
}

function initTheme() {
  const theme = getTheme();
  document.documentElement.setAttribute('data-theme', theme);
}

function toggleTheme() {
  const current = getTheme();
  const themes = ['dark', 'light', 'high-contrast'];
  const nextIndex = (themes.indexOf(current) + 1) % themes.length;
  setTheme(themes[nextIndex]);
}

function broadcastThemeChange(theme) {
  // Broadcast to other tabs
  localStorage.setItem('ps_theme_broadcast', Date.now() + ':' + theme);
}

// Listen for theme changes from other tabs
window.addEventListener('storage', (e) => {
  if (e.key === 'ps_theme_broadcast') {
    const [, theme] = e.newValue.split(':');
    document.documentElement.setAttribute('data-theme', theme);
  }
});

// ── Session Management ──────────────────────────────────────────────────────
let sessionTimer = null;
let sessionWarningShown = false;
const SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
const WARNING_TIME = 5 * 60 * 1000; // 5 minutes before timeout

function resetSessionTimer() {
  clearTimeout(sessionTimer);
  sessionWarningShown = false;
  
  sessionTimer = setTimeout(() => {
    if (!sessionWarningShown) {
      sessionWarningShown = true;
      showSessionWarning();
    }
  }, SESSION_TIMEOUT - WARNING_TIME);
}

function showSessionWarning() {
  const remaining = Math.floor(WARNING_TIME / 1000 / 60);
  toast(`Session expires in ${remaining} minutes. Click to extend.`, 'warn', 10000);
  
  setTimeout(() => {
    if (isLoggedIn()) {
      toast('Session expired. Redirecting to login...', 'error');
      setTimeout(logout, 2000);
    }
  }, WARNING_TIME);
}

// Reset timer on user activity
['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'].forEach(event => {
  document.addEventListener(event, resetSessionTimer, { passive: true });
});

// ── Mobile Navigation ───────────────────────────────────────────────────────
function toggleMobileMenu() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.querySelector('.mobile-overlay');
  const main = document.querySelector('.main');
  
  if (sidebar && overlay && main) {
    const isVisible = sidebar.classList.contains('mobile-visible');
    
    if (isVisible) {
      sidebar.classList.remove('mobile-visible');
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    } else {
      sidebar.classList.add('mobile-visible');
      overlay.classList.add('active');
      document.body.style.overflow = 'hidden';
    }
  }
}

function closeMobileMenu() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.querySelector('.mobile-overlay');
  
  if (sidebar && overlay) {
    sidebar.classList.remove('mobile-visible');
    overlay.classList.remove('active');
    document.body.style.overflow = '';
  }
}

// ── Enhanced Auth ────────────────────────────────────────────────────────────
function getToken()      { return localStorage.getItem('ps_token'); }
function getEmail()      { return localStorage.getItem('ps_email') || ''; }
function getMerchantId() { return localStorage.getItem('ps_merchant_id') || ''; }
function isLoggedIn()    { return !!getToken(); }
function isDemoMode()    { return getToken() === 'demo-mode'; }

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

// ── Enhanced API fetch with retry and caching ───────────────────────────────
const apiCache = new Map();
const CACHE_DURATION = 30000; // 30 seconds

async function apiFetch(path, opts = {}) {
  // In demo mode, never hit the real backend — return undefined immediately
  if (isDemoMode()) return undefined;

  // Check cache for GET requests
  if (!opts.method || opts.method === 'GET') {
    const cacheKey = path + JSON.stringify(opts);
    const cached = apiCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
      return cached.data;
    }
  }

  const maxRetries = opts.retries || 2;
  let lastError;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
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
        // Token expired — redirect to login
        clearAuth();
        const loginPath = window.location.pathname.includes('/pages/') ? '../index.html' : 'index.html';
        window.location.href = loginPath;
        return null;
      }

      if (!res.ok) {
        console.warn(`API ${path} returned ${res.status}`);
        if (attempt === maxRetries) return null;
        continue;
      }

      const text = await res.text();
      const data = text ? JSON.parse(text) : {};
      
      // Cache successful GET requests
      if (!opts.method || opts.method === 'GET') {
        const cacheKey = path + JSON.stringify(opts);
        apiCache.set(cacheKey, { data, timestamp: Date.now() });
      }
      
      return data;

    } catch (e) {
      lastError = e;
      if (attempt < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, 1000 * (attempt + 1)));
        continue;
      }
    }
  }

  // Network error — server unreachable
  console.warn(`API ${path} unreachable after ${maxRetries + 1} attempts:`, lastError?.message);
  return undefined;
}

// Clear API cache
function clearApiCache() {
  apiCache.clear();
}

// ── WebSocket Simulation for Real-time Updates ─────────────────────────────
class MockWebSocket {
  constructor() {
    this.listeners = new Map();
    this.connected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
  }

  connect() {
    this.connected = true;
    this.reconnectAttempts = 0;
    this.emit('connect');
    
    // Simulate real-time updates
    this.startSimulation();
  }

  disconnect() {
    this.connected = false;
    this.emit('disconnect');
    if (this.simulationTimer) {
      clearInterval(this.simulationTimer);
    }
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event) || [];
    callbacks.forEach(callback => callback(data));
  }

  startSimulation() {
    this.simulationTimer = setInterval(() => {
      if (!this.connected) return;

      // Simulate random events
      const events = [
        { type: 'transaction', data: this.generateMockTransaction() },
        { type: 'fraud_alert', data: this.generateMockFraudAlert() },
        { type: 'system_status', data: this.generateSystemStatus() }
      ];

      const randomEvent = events[Math.floor(Math.random() * events.length)];
      this.emit(randomEvent.type, randomEvent.data);
    }, 5000 + Math.random() * 10000); // Random interval 5-15 seconds
  }

  generateMockTransaction() {
    const methods = ['CARD', 'UPI', 'NET_BANKING', 'WALLET', 'BANK_TRANSFER'];
    const statuses = ['COMPLETED', 'FAILED', 'PENDING'];
    
    return {
      id: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
      amount: (Math.random() * 50000 + 100).toFixed(2),
      paymentMethod: methods[Math.floor(Math.random() * methods.length)],
      status: statuses[Math.floor(Math.random() * statuses.length)],
      fraudScore: (Math.random() * 100).toFixed(1),
      fraudFlagged: Math.random() > 0.9,
      customerEmail: `user${Math.floor(Math.random() * 1000)}@example.com`,
      initiatedAt: new Date().toISOString()
    };
  }

  generateMockFraudAlert() {
    const rules = [['HIGH_AMOUNT'], ['VELOCITY_COUNT'], ['GEO_MISMATCH'], ['CARD_TESTING']];
    const decisions = ['REJECT', 'FLAG', 'REVIEW'];
    
    return {
      transactionId: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
      score: (75 + Math.random() * 25).toFixed(1),
      decision: decisions[Math.floor(Math.random() * decisions.length)],
      ruleTriggers: rules[Math.floor(Math.random() * rules.length)],
      amount: (Math.random() * 100000 + 5000).toFixed(2),
      createdAt: new Date().toISOString()
    };
  }

  generateSystemStatus() {
    return {
      services: ['auth', 'payment', 'fraud', 'reconciliation', 'notification', 'reporting'].map(name => ({
        name,
        status: Math.random() > 0.1 ? 'UP' : 'DOWN',
        responseTime: Math.floor(Math.random() * 200 + 50)
      }))
    };
  }
}

const mockWS = new MockWebSocket();

// ── Real-time Notifications ─────────────────────────────────────────────────
let notificationCount = 0;
const MAX_NOTIFICATIONS = 50;
const notifications = [];

function addNotification(notification) {
  const id = Date.now() + Math.random();
  const newNotif = {
    id,
    ...notification,
    timestamp: new Date().toISOString(),
    read: false
  };
  
  notifications.unshift(newNotif);
  if (notifications.length > MAX_NOTIFICATIONS) {
    notifications.splice(MAX_NOTIFICATIONS);
  }
  
  notificationCount++;
  updateNotificationBadge();
  
  // Show toast for important notifications
  if (notification.type === 'fraud' || notification.type === 'error') {
    toast(notification.title, notification.type === 'fraud' ? 'warn' : 'error');
  }
  
  return id;
}

function markNotificationRead(id) {
  const notification = notifications.find(n => n.id === id);
  if (notification && !notification.read) {
    notification.read = true;
    notificationCount = Math.max(0, notificationCount - 1);
    updateNotificationBadge();
  }
}

function markAllNotificationsRead() {
  notifications.forEach(n => n.read = true);
  notificationCount = 0;
  updateNotificationBadge();
}

function updateNotificationBadge() {
  const badge = document.querySelector('.notification-badge');
  if (badge) {
    if (notificationCount > 0) {
      badge.textContent = notificationCount > 99 ? '99+' : notificationCount;
      badge.style.display = 'block';
    } else {
      badge.style.display = 'none';
    }
  }
}

function getNotifications() {
  return notifications;
}

// ── Data Export Functionality ───────────────────────────────────────────────
function exportToCSV(data, filename) {
  if (!data || !data.length) {
    toast('No data to export', 'warn');
    return;
  }

  const headers = Object.keys(data[0]);
  const csvContent = [
    headers.join(','),
    ...data.map(row => 
      headers.map(header => {
        const value = row[header];
        // Escape commas and quotes
        if (typeof value === 'string' && (value.includes(',') || value.includes('"'))) {
          return `"${value.replace(/"/g, '""')}"`;
        }
        return value;
      }).join(',')
    )
  ].join('\n');

  downloadFile(csvContent, filename + '.csv', 'text/csv');
}

function exportToJSON(data, filename) {
  if (!data) {
    toast('No data to export', 'warn');
    return;
  }

  const jsonContent = JSON.stringify(data, null, 2);
  downloadFile(jsonContent, filename + '.json', 'application/json');
}

function downloadFile(content, filename, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  
  toast(`Downloaded ${filename}`, 'success');
}

// ── Advanced Filtering ──────────────────────────────────────────────────────
class AdvancedFilter {
  constructor() {
    this.filters = new Map();
    this.savedFilters = this.loadSavedFilters();
  }

  addFilter(key, value, operator = 'equals') {
    this.filters.set(key, { value, operator });
  }

  removeFilter(key) {
    this.filters.delete(key);
  }

  clearFilters() {
    this.filters.clear();
  }

  applyFilters(data) {
    return data.filter(item => {
      for (const [key, filter] of this.filters) {
        if (!this.matchesFilter(item[key], filter.value, filter.operator)) {
          return false;
        }
      }
      return true;
    });
  }

  matchesFilter(itemValue, filterValue, operator) {
    if (filterValue === '' || filterValue == null) return true;

    switch (operator) {
      case 'equals':
        return itemValue == filterValue;
      case 'contains':
        return String(itemValue).toLowerCase().includes(String(filterValue).toLowerCase());
      case 'startsWith':
        return String(itemValue).toLowerCase().startsWith(String(filterValue).toLowerCase());
      case 'greaterThan':
        return Number(itemValue) > Number(filterValue);
      case 'lessThan':
        return Number(itemValue) < Number(filterValue);
      case 'between':
        const [min, max] = filterValue.split(',').map(Number);
        return Number(itemValue) >= min && Number(itemValue) <= max;
      case 'dateAfter':
        return new Date(itemValue) > new Date(filterValue);
      case 'dateBefore':
        return new Date(itemValue) < new Date(filterValue);
      default:
        return true;
    }
  }

  saveFilter(name) {
    this.savedFilters[name] = Object.fromEntries(this.filters);
    localStorage.setItem('ps_saved_filters', JSON.stringify(this.savedFilters));
    toast(`Filter "${name}" saved`, 'success');
  }

  loadFilter(name) {
    const filter = this.savedFilters[name];
    if (filter) {
      this.filters = new Map(Object.entries(filter));
      toast(`Filter "${name}" loaded`, 'success');
    }
  }

  loadSavedFilters() {
    try {
      return JSON.parse(localStorage.getItem('ps_saved_filters') || '{}');
    } catch {
      return {};
    }
  }

  getSavedFilters() {
    return Object.keys(this.savedFilters);
  }
}

// ── Virtual Scrolling for Large Datasets ───────────────────────────────────
class VirtualScroller {
  constructor(container, itemHeight, renderItem) {
    this.container = container;
    this.itemHeight = itemHeight;
    this.renderItem = renderItem;
    this.data = [];
    this.visibleStart = 0;
    this.visibleEnd = 0;
    this.scrollTop = 0;
    
    this.setupContainer();
    this.bindEvents();
  }

  setupContainer() {
    this.container.style.position = 'relative';
    this.container.style.overflow = 'auto';
    
    this.content = document.createElement('div');
    this.content.style.position = 'relative';
    this.container.appendChild(this.content);
  }

  bindEvents() {
    this.container.addEventListener('scroll', () => {
      this.scrollTop = this.container.scrollTop;
      this.render();
    });
  }

  setData(data) {
    this.data = data;
    this.content.style.height = `${data.length * this.itemHeight}px`;
    this.render();
  }

  render() {
    const containerHeight = this.container.clientHeight;
    const visibleStart = Math.floor(this.scrollTop / this.itemHeight);
    const visibleEnd = Math.min(
      visibleStart + Math.ceil(containerHeight / this.itemHeight) + 1,
      this.data.length
    );

    // Only re-render if visible range changed
    if (visibleStart !== this.visibleStart || visibleEnd !== this.visibleEnd) {
      this.visibleStart = visibleStart;
      this.visibleEnd = visibleEnd;

      // Clear existing items
      this.content.innerHTML = '';

      // Render visible items
      for (let i = visibleStart; i < visibleEnd; i++) {
        const item = document.createElement('div');
        item.style.position = 'absolute';
        item.style.top = `${i * this.itemHeight}px`;
        item.style.left = '0';
        item.style.right = '0';
        item.style.height = `${this.itemHeight}px`;
        item.innerHTML = this.renderItem(this.data[i], i);
        this.content.appendChild(item);
      }
    }
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

// ── Enhanced Sidebar with Mobile Support ────────────────────────────────────
function sidebar(active) {
  const email = getEmail();
  const init  = email ? email[0].toUpperCase() : 'A';
  const badge = getFraudBadge();
  const demo  = isDemoMode();
  const theme = getTheme();
  const themeIcon = theme === 'light' ? '☀️' : theme === 'high-contrast' ? '🔆' : '🌙';

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
      <a class="nav-link ${active === l.id ? 'active' : ''}" href="${l.href}" onclick="closeMobileMenu()">
        <div class="nav-icon">${l.icon}</div>
        <span>${l.label}</span>
        ${l.badge ? `<span class="nav-badge">${l.badge}</span>` : ''}
      </a>`).join('')}
    <div class="nav-group-label">Settings</div>
    <div class="nav-link" onclick="toggleTheme()">
      <div class="nav-icon">${themeIcon}</div>
      <span>Theme</span>
    </div>
    <div class="sidebar-bottom">
      ${demo ? `<div style="padding:8px 10px;margin-bottom:8px;background:rgba(245,158,11,.08);border:1px solid rgba(245,158,11,.2);border-radius:6px;font-size:11px;color:var(--amber);text-align:center">⚠ Demo Mode</div>` : ''}
      <div class="user-row">
        <div class="user-av">${init}</div>
        <div class="user-meta">
          <div class="user-name">${email}</div>
          <div class="user-role">${demo ? 'Demo' : 'Admin'}</div>
        </div>
        <button class="btn-signout" onclick="logout()" title="Sign out">→</button>
      </div>
    </div>`;
}

function initPage(id) {
  requireAuth();
  initTheme();
  resetSessionTimer();
  
  const s = document.getElementById('sidebar');
  if (s) s.innerHTML = sidebar(id);
  
  // Add mobile overlay if it doesn't exist
  if (!document.querySelector('.mobile-overlay')) {
    const overlay = document.createElement('div');
    overlay.className = 'mobile-overlay';
    overlay.onclick = closeMobileMenu;
    document.body.appendChild(overlay);
  }
  
  // Initialize WebSocket simulation
  if (!mockWS.connected) {
    mockWS.connect();
    setupWebSocketListeners();
  }
}

function setupWebSocketListeners() {
  mockWS.on('transaction', (data) => {
    addNotification({
      type: 'transaction',
      title: 'New Transaction',
      message: `${fmt(data.amount)} via ${data.paymentMethod}`,
      icon: '💳'
    });
  });

  mockWS.on('fraud_alert', (data) => {
    addNotification({
      type: 'fraud',
      title: 'Fraud Alert',
      message: `Transaction ${data.transactionId} flagged (Score: ${data.score})`,
      icon: '🚨'
    });
    
    // Update fraud badge
    const currentBadge = getFraudBadge();
    setFraudBadge(currentBadge + 1);
    
    // Update sidebar if visible
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
      const currentPage = document.body.getAttribute('data-page') || 'overview';
      sidebar.innerHTML = sidebar(currentPage);
    }
  });

  mockWS.on('system_status', (data) => {
    const downServices = data.services.filter(s => s.status === 'DOWN');
    if (downServices.length > 0) {
      addNotification({
        type: 'system',
        title: 'Service Alert',
        message: `${downServices.length} service(s) down: ${downServices.map(s => s.name).join(', ')}`,
        icon: '⚠️'
      });
    }
  });
}

// ── Enhanced Mock Data with More Variety ────────────────────────────────────
function mockDashboard() {
  const now = Date.now();
  const dayMs = 86400000;
  
  return {
    totalVolume: 45230000 + Math.floor(Math.random() * 5000000),
    totalTransactions: 12445 + Math.floor(Math.random() * 1000),
    successfulTransactions: 11698 + Math.floor(Math.random() * 800),
    failedTransactions: 747 + Math.floor(Math.random() * 200),
    fraudFlagged: 83 + Math.floor(Math.random() * 20),
    successRate: 94.0 + (Math.random() * 4 - 2),
    avgTransactionValue: 3634 + Math.floor(Math.random() * 1000 - 500),
    dailyVolumes: Array.from({ length: 14 }, (_, i) => ({
      date: new Date(now - (13 - i) * dayMs).toISOString().slice(0, 10),
      volume: Math.floor(Math.random() * 3000000 + 1500000),
      transactionCount: Math.floor(Math.random() * 400 + 600),
      successRate: 85 + Math.random() * 15,
      fraudCount: Math.floor(Math.random() * 50 + 10)
    })),
    hourlyData: Array.from({ length: 24 }, (_, hour) => ({
      hour,
      volume: Math.floor(Math.random() * 200000 + 50000),
      transactions: Math.floor(Math.random() * 100 + 20),
      fraudScore: Math.random() * 30 + 10
    })),
    paymentMethods: [
      { method: 'CARD', volume: 38, amount: 17000000, avgTicket: 4500 },
      { method: 'UPI', volume: 31, amount: 14000000, avgTicket: 2800 },
      { method: 'NET_BANKING', volume: 14, amount: 6300000, avgTicket: 5200 },
      { method: 'WALLET', volume: 10, amount: 4500000, avgTicket: 1800 },
      { method: 'BANK_TRANSFER', volume: 7, amount: 3200000, avgTicket: 8900 }
    ]
  };
}

function mockTransactions(n = 15) {
  const statuses = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'FAILED', 'PENDING', 'FRAUD_CHECK'];
  const methods = ['CARD', 'UPI', 'NET_BANKING', 'WALLET', 'BANK_TRANSFER'];
  const countries = ['IN', 'US', 'GB', 'SG', 'AE'];
  const devices = ['Mobile', 'Desktop', 'Tablet'];
  
  return Array.from({ length: n }, (_, i) => ({
    id: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
    amount: (Math.random() * 80000 + 500).toFixed(2),
    paymentMethod: methods[Math.floor(Math.random() * methods.length)],
    customerEmail: `user${i + 1}@example.com`,
    fraudScore: (Math.random() * 100).toFixed(1),
    fraudFlagged: Math.random() > 0.85,
    status: statuses[Math.floor(Math.random() * statuses.length)],
    initiatedAt: new Date(Date.now() - i * 3600000 * 1.5).toISOString(),
    completedAt: Math.random() > 0.3 ? new Date(Date.now() - i * 3600000 * 1.5 + 300000).toISOString() : null,
    country: countries[Math.floor(Math.random() * countries.length)],
    device: devices[Math.floor(Math.random() * devices.length)],
    ipAddress: `${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    merchantId: 'MERCH_' + Math.random().toString(36).slice(2, 8).toUpperCase(),
    currency: 'INR',
    description: `Payment for order #${Math.floor(Math.random() * 10000)}`,
    riskFactors: Math.random() > 0.7 ? ['HIGH_AMOUNT', 'NEW_DEVICE'] : [],
    processingTime: Math.floor(Math.random() * 5000 + 100) // ms
  }));
}

function mockFraud(n = 10) {
  const rules = [
    ['HIGH_AMOUNT'], 
    ['VELOCITY_COUNT', 'NEW_DEVICE'], 
    ['GEO_MISMATCH'], 
    ['CARD_TESTING'], 
    ['HIGH_AMOUNT', 'VELOCITY_COUNT'],
    ['SUSPICIOUS_EMAIL'],
    ['BLACKLISTED_IP'],
    ['UNUSUAL_PATTERN']
  ];
  const decisions = ['REJECT', 'FLAG', 'REVIEW', 'APPROVE'];
  const riskLevels = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  
  return Array.from({ length: n }, (_, i) => ({
    transactionId: 'TXN' + Math.random().toString(36).slice(2, 10).toUpperCase(),
    score: (Math.random() * 100).toFixed(1),
    decision: decisions[Math.floor(Math.random() * decisions.length)],
    riskLevel: riskLevels[Math.floor(Math.random() * riskLevels.length)],
    ruleTriggers: rules[Math.floor(Math.random() * rules.length)],
    amount: (Math.random() * 100000 + 1000).toFixed(2),
    customerEmail: `user${i + 1}@example.com`,
    ipAddress: `${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    country: ['IN', 'US', 'GB', 'CN', 'RU'][Math.floor(Math.random() * 5)],
    deviceFingerprint: Math.random().toString(36).slice(2, 12),
    createdAt: new Date(Date.now() - i * 7200000).toISOString(),
    reviewedAt: Math.random() > 0.5 ? new Date(Date.now() - i * 7200000 + 1800000).toISOString() : null,
    reviewedBy: Math.random() > 0.5 ? 'admin@payshield.com' : null,
    notes: Math.random() > 0.7 ? 'Requires manual verification' : null
  }));
}

// ── Performance Monitoring ──────────────────────────────────────────────────
class PerformanceMonitor {
  constructor() {
    this.metrics = new Map();
    this.startTimes = new Map();
  }

  start(label) {
    this.startTimes.set(label, performance.now());
  }

  end(label) {
    const startTime = this.startTimes.get(label);
    if (startTime) {
      const duration = performance.now() - startTime;
      this.metrics.set(label, duration);
      this.startTimes.delete(label);
      
      // Log slow operations
      if (duration > 1000) {
        console.warn(`Slow operation: ${label} took ${duration.toFixed(2)}ms`);
      }
      
      return duration;
    }
    return 0;
  }

  getMetrics() {
    return Object.fromEntries(this.metrics);
  }

  clear() {
    this.metrics.clear();
    this.startTimes.clear();
  }
}

const perfMonitor = new PerformanceMonitor();

// ── Accessibility Helpers ───────────────────────────────────────────────────
function announceToScreenReader(message) {
  const announcement = document.createElement('div');
  announcement.setAttribute('aria-live', 'polite');
  announcement.setAttribute('aria-atomic', 'true');
  announcement.style.position = 'absolute';
  announcement.style.left = '-10000px';
  announcement.style.width = '1px';
  announcement.style.height = '1px';
  announcement.style.overflow = 'hidden';
  
  document.body.appendChild(announcement);
  announcement.textContent = message;
  
  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
}

function setupKeyboardNavigation() {
  document.addEventListener('keydown', (e) => {
    // Escape key closes modals and menus
    if (e.key === 'Escape') {
      closeMobileMenu();
      closeAllModals();
    }
    
    // Alt + M toggles mobile menu
    if (e.altKey && e.key === 'm') {
      e.preventDefault();
      toggleMobileMenu();
    }
    
    // Alt + T toggles theme
    if (e.altKey && e.key === 't') {
      e.preventDefault();
      toggleTheme();
    }
  });
}

function closeAllModals() {
  document.querySelectorAll('.modal-overlay').forEach(modal => {
    modal.classList.add('hidden');
  });
}

// Initialize accessibility features
document.addEventListener('DOMContentLoaded', setupKeyboardNavigation);

// ── Skeleton loader ───────────────────────────────────────────────────────────
function skeletonKpi() {
  return `<div class="skeleton" style="height:26px;width:80%;border-radius:4px;margin-bottom:6px"></div>
          <div class="skeleton" style="height:13px;width:55%;border-radius:4px"></div>`;
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
    <div class="refresh-ring" title="Auto-refreshes every ${intervalSec}s — click to refresh now">
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
