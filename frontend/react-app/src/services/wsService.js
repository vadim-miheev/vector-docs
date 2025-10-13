import { WS_NOTIFICATIONS_URL } from '../config/endpoints';

export function connectNotifications(onMessage) {
  if (typeof window === 'undefined' || !('WebSocket' in window)) {
    // No-op on server/test environments
    return { close: () => {}, send: () => {}, state: () => 0 };
  }
  const ws = new WebSocket(WS_NOTIFICATIONS_URL);
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onMessage?.(data);
    } catch (_) {
      onMessage?.(event.data);
    }
  };
  ws.onerror = (e) => {
    console.log("WebSocket error:", e)
  };
  return {
    send: (payload) => ws.readyState === 1 && ws.send(typeof payload === 'string' ? payload : JSON.stringify(payload)),
    close: () => ws.close(),
    state: () => ws.readyState
  };
}
