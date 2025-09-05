import React from 'react';
import { useNotifications } from '../store/NotificationsContext';

export default function NotificationToast() {
  const { messages, removeMessage } = useNotifications();
  if (!messages.length) return null;
  return (
    <div style={{ position: 'fixed', right: 16, bottom: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
      {messages.map((m) => (
        <div key={m.id} style={{ background: '#222', color: '#fff', padding: '8px 12px', borderRadius: 6, boxShadow: '0 2px 8px rgba(0,0,0,0.25)' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ flex: 1 }}>{m.text}</span>
            <button onClick={() => removeMessage(m.id)} style={{ background: 'transparent', color: '#fff', border: 'none', cursor: 'pointer' }}>x</button>
          </div>
        </div>
      ))}
    </div>
  );
}
