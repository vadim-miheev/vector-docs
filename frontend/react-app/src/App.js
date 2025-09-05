import React from 'react';
import './App.css';
import { AuthProvider } from './store/AuthContext';
import { NotificationsProvider } from './store/NotificationsContext';
import AppRouter from './app/AppRouter';
import NotificationToast from './components/NotificationToast';
import { useWebSocket } from './hooks/useWebSocket';

function AppShell() {
  // Initialize WebSocket notifications when authenticated
  useWebSocket();
  return (
    <>
      <AppRouter />
      <NotificationToast />
    </>
  );
}

function App() {
  return (
    <AuthProvider>
      <NotificationsProvider>
        <AppShell />
      </NotificationsProvider>
    </AuthProvider>
  );
}

export default App;
