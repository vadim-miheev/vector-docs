import {useEffect, useRef} from 'react';
import {connectNotifications} from '../services/wsService';
import {useNotifications} from '../store/NotificationsContext';
import {useAuthContext} from '../store/AuthContext';


let wsInstance = null;
let wsConnecting = false;
let wsConsumers = 0;

export function useWebSocket() {
  const { addMessage } = useNotifications();
  const addMessageRef = useRef(addMessage);
  const { isAuthenticated } = useAuthContext();
  const connRef = useRef(null);

  useEffect(() => {
    addMessageRef.current = addMessage;
  }, [addMessage]);

  useEffect(() => {
    wsConsumers += 1;

    if (!isAuthenticated) {
      if (wsInstance) {
        try { wsInstance.close?.(); } catch {}
      }
      wsInstance = null;
      wsConnecting = false;
      connRef.current = null;

      return () => {
        wsConsumers = Math.max(0, wsConsumers - 1);
      };
    }

    const state = wsInstance?.state();
    const wsExists = wsInstance && (state === WebSocket.OPEN || state === WebSocket.CONNECTING);

    if (!wsConnecting && !wsExists) {
      wsConnecting = true;

      wsInstance = connectNotifications((payload) => {
        const eventName = typeof payload === 'object' && payload ? payload.event : undefined;

        const handlers = {
          'documents.processed': () => {
            if (typeof window !== 'undefined') {
              window.dispatchEvent(
                new CustomEvent('documents:update', { detail: { id: payload?.id } })
              );
              addMessageRef.current(`Document ${payload?.name} ready for search`);
            }
          },
          'chat.response': () => {
            if (typeof window !== 'undefined') {
              window.dispatchEvent(
                new CustomEvent('chat:response', { detail: payload })
              );
            }
          },
        };

        if (eventName && handlers[eventName]) {
          handlers[eventName]();
          return;
        }

        const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
        addMessageRef.current(text);
      });

      wsInstance?.addEventListener?.('open', () => { wsConnecting = false; });
      wsInstance?.addEventListener?.('close', () => { wsConnecting = false; });
      wsInstance?.addEventListener?.('error', () => { wsConnecting = false; });
    }

    connRef.current = wsInstance;

    return () => {
      wsConsumers = Math.max(0, wsConsumers - 1);
      if (wsConsumers === 0 && !isAuthenticated) {
        try { wsInstance?.close?.(); } catch {}
        wsInstance = null;
        wsConnecting = false;
      }
    };
  }, [isAuthenticated]);

  return connRef.current;
}
