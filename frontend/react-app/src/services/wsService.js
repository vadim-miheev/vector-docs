import { WS_NOTIFICATIONS_URL } from '../config/endpoints';

export function connectNotifications(onMessage) {
  // SSR/test-safe no-op with the same API shape
  if (typeof window === 'undefined' || !('WebSocket' in window)) {
    const listeners = new Map();
    return {
      send: () => false,
      close: () => {},
      state: () => 0, // CONNECTING to prevent duplicate connectors in SSR
      addEventListener: (type, listener) => {
        const set = listeners.get(type) || new Set();
        set.add(listener);
        listeners.set(type, set);
      },
      removeEventListener: (type, listener) => {
        const set = listeners.get(type);
        if (set) set.delete(listener);
      }
    };
  }

  let ws = null;
  let shouldReconnect = true;
  let reconnectAttempts = 0;

  const baseDelay = 500; // ms
  const maxDelay = 30000; // ms

  // Store external listeners so we can reattach them on reconnect
  const listeners = new Map(); // Map<string, Set<Function>>

  function attachStoredListeners(socket) {
    listeners.forEach((set, type) => {
      set.forEach((listener) => {
        try { socket.addEventListener(type, listener); } catch (_) {}
      });
    });
  }

  function scheduleReconnect() {
    if (!shouldReconnect) return;
    reconnectAttempts += 1;
    const delay = Math.min(maxDelay, baseDelay * Math.pow(2, reconnectAttempts - 1));
    const jitter = Math.random() * delay * 0.3; // up to 30% jitter
    const wait = Math.round(delay / 2 + jitter);
    setTimeout(() => {
      createSocket();
    }, wait);
  }

  function createSocket() {
    if (!shouldReconnect) return;
    try {
      ws = new WebSocket(WS_NOTIFICATIONS_URL);

      // Internal handlers
      ws.addEventListener('open', () => {
        reconnectAttempts = 0;
      });

      ws.addEventListener('message', (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessage?.(data);
        } catch (_) {
          onMessage?.(event.data);
        }
      });

      ws.addEventListener('error', (e) => {
        console.log('WebSocket error:', e);
      });

      ws.addEventListener('close', () => {
        if (!shouldReconnect) return;
        scheduleReconnect();
      });

      // Attach any previously registered external listeners
      attachStoredListeners(ws);
    } catch (e) {
      console.log('WebSocket create error:', e);
      scheduleReconnect();
    }
  }

  // Start initial connection
  createSocket();

  return {
    send: (payload) => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
        return true;
      }
      return false;
    },
    close: () => {
      shouldReconnect = false;
      try { ws?.close(); } catch (_) {}
    },
    state: () => {
      if (ws) return ws.readyState;
      return shouldReconnect ? WebSocket.CONNECTING : WebSocket.CLOSED;
    },
    addEventListener: (type, listener) => {
      let set = listeners.get(type);
      if (!set) {
        set = new Set();
        listeners.set(type, set);
      }
      if (typeof listener === 'function') {
        set.add(listener);
      }
      try { ws?.addEventListener?.(type, listener); } catch (_) {}
    },
    removeEventListener: (type, listener) => {
      const set = listeners.get(type);
      if (set) {
        set.delete(listener);
        if (set.size === 0) listeners.delete(type);
      }
      try { ws?.removeEventListener?.(type, listener); } catch (_) {}
    }
  };
}
