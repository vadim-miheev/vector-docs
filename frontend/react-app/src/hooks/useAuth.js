import { useCallback } from 'react';
import { useAuthContext } from '../store/AuthContext';
import { authService } from '../services/authService';
import {useNotifications} from "../store/NotificationsContext";

function clearTokenCookie() {
  try {
    document.cookie = 'vd_token=; Max-Age=0; Path=/; SameSite=Lax';
  } catch (_) {}
}

export function useAuth() {
  const { user, setUser, isAuthenticated } = useAuthContext();
  const { addMessage } = useNotifications();

  const login = useCallback(async (email, password) => {
    const u = await authService.login({ email, password });
    if (u.error !== undefined) {
      addMessage(u.error);
      return null;
    }
    setUser(u);
    return u;
  }, [setUser, addMessage]);

  const register = useCallback(async (email) => {
    const res = await authService.register({ email });
    if (res.error !== undefined) {
      addMessage(res.error);
      return null;
    }
    addMessage(res.message || 'Please check your email to complete registration');
    return res;
  }, [addMessage]);

  const logout = useCallback(() => {
    clearTokenCookie();
    setUser(null);
  }, [setUser]);

  return { user, isAuthenticated, login, register, logout };
}
