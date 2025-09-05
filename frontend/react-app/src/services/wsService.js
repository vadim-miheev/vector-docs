import { WS_NOTIFICATIONS_URL } from '../config/endpoints';

export function connectNotifications(onMessage) {
  if (typeof window === 'undefined' || !('WebSocket' in window)) {
    // No-op on server/test environments
    return { close: () => {}, send: () => {} };
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
  ws.onerror = () => {
    // silently ignore for demo
  };
  return {
    send: (payload) => ws.readyState === 1 && ws.send(typeof payload === 'string' ? payload : JSON.stringify(payload)),
    close: () => ws.close(),
  };
}
