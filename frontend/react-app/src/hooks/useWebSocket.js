import { useEffect, useRef } from 'react';
import { connectNotifications } from '../services/wsService';
import { useNotifications } from '../store/NotificationsContext';
import { useAuthContext } from '../store/AuthContext';

export function useWebSocket() {
  const { addMessage } = useNotifications();
  const { isAuthenticated } = useAuthContext();
  const connRef = useRef(null);

  useEffect(() => {
    if (!isAuthenticated) {
      // Close if exists when user logs out
      connRef.current?.close();
      connRef.current = null;
      return;
    }
    const conn = connectNotifications((payload) => {
      const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
      addMessage(text);
    });
    connRef.current = conn;
    return () => conn.close();
  }, [isAuthenticated, addMessage]);

  return connRef.current;
}
