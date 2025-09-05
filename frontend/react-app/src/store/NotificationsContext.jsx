import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

const NotificationsContext = createContext(null);

export function NotificationsProvider({ children }) {
  const [messages, setMessages] = useState([]);

  const addMessage = useCallback((msg) => {
    const id = Date.now() + Math.random();
    setMessages((prev) => [...prev, { id, text: String(msg) }]);
    return id;
  }, []);

  const removeMessage = useCallback((id) => {
    setMessages((prev) => prev.filter((m) => m.id !== id));
  }, []);

  const value = useMemo(() => ({ messages, addMessage, removeMessage }), [messages, addMessage, removeMessage]);

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}

export function useNotifications() {
  const ctx = useContext(NotificationsContext);
  if (!ctx) throw new Error('useNotifications must be used within NotificationsProvider');
  return ctx;
}
