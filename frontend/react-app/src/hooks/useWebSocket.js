import { useEffect, useRef } from 'react';
import { connectNotifications } from '../services/wsService';
import { useNotifications } from '../store/NotificationsContext';
import { useAuthContext } from '../store/AuthContext';
import { useDocuments} from "./useDocuments";

export function useWebSocket() {
  const { addMessage } = useNotifications();
  const { isAuthenticated } = useAuthContext();
  const { refresh } = useDocuments();
  const connRef = useRef(null);

  useEffect(() => {
    if (!isAuthenticated) {
      // Close if exists when user logs out
      connRef.current?.close();
      connRef.current = null;
      return;
    }

    const conn = connectNotifications((payload) => {
      const eventName = typeof payload === 'object' && payload ? payload.event : undefined;

      // Extensible event handlers map
      const handlers = {
        'documents.processed': () => {
          // Remove document from a list
          if (typeof window !== 'undefined') {
              window.dispatchEvent(
                  new CustomEvent('documents:update', {
                      detail: { id: payload?.id }
                  })
              );
              addMessage(`Document ${payload?.name} ready for search`)
          }
        },
      };

      if (eventName && handlers[eventName]) {
        handlers[eventName]();
        return; // handled
      }

      // Default behavior: show the message as a notification
      const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
      addMessage(text);
    });

    connRef.current = conn;
    return () => conn.close();
  }, [isAuthenticated, addMessage]);

  return connRef.current;
}
