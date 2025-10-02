import { useCallback } from 'react';
import { useAuthContext } from '../store/AuthContext';
import { authService } from '../services/authService';
import {useNotifications} from "../store/NotificationsContext";

export function useAuth() {
  const { user, setUser, isAuthenticated } = useAuthContext();
    const { addMessage } = useNotifications();

  const login = useCallback(async (email, password) => {
    const u = await authService.login({ email, password });
    if (u.id === 'local') {
        addMessage("Wrong credentials.");
        return null;
    }
    setUser(u);
    return u;
  }, [setUser]);

  const register = useCallback(async (email, password) => {
    const u = await authService.register({ email, password });
      if (u.id === 'local') {
          addMessage("Wrong credentials.");
          return null;
      }
    setUser(u);
    return u;
  }, [setUser]);

  const logout = useCallback(() => {
    setUser(null);
  }, [setUser]);

  return { user, isAuthenticated, login, register, logout };
}
