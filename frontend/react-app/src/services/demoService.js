// Centralized utilities for demo mode logic and demo.json loading

let demoJsonCache = null;
let demoJsonPromise = null;

export function getDemoId() {
  const demoIdStr = process.env.REACT_APP_DEMO_USER_ID || '0';
  const demoId = parseInt(demoIdStr, 10);
  return Number.isFinite(demoId) ? demoId : 0;
}

export function isDemoEnabled() {
  const id = getDemoId();
  return id > 0;
}

export function isDemoUser(user) {
  if (!isDemoEnabled()) return false;
  const id = getDemoId();
  return String(user?.id ?? '') === String(id);
}

// Cached loader for /demo.json; returns parsed object or null on error
export function fetchDemoJson(force = false) {
  if (!isDemoEnabled() && !force) {
    // If demo is disabled, we can short-circuit with null
    return Promise.resolve(null);
  }

  if (!force && demoJsonCache !== null) {
    return Promise.resolve(demoJsonCache);
  }
  if (!force && demoJsonPromise) {
    return demoJsonPromise;
  }

  demoJsonPromise = fetch('/demo.json')
    .then((r) => (r.ok ? r.json() : null))
    .then((data) => {
      demoJsonCache = data;
      demoJsonPromise = null;
      return data;
    })
    .catch(() => {
      demoJsonPromise = null;
      return null;
    });

  return demoJsonPromise;
}
