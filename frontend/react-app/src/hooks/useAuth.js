import { useCallback } from 'react';
import { useAuthContext } from '../store/AuthContext';
import { authService } from '../services/authService';

export function useAuth() {
  const { user, setUser, isAuthenticated } = useAuthContext();

  const login = useCallback(async (email, password) => {
    const u = await authService.login({ email, password });
    setUser(u);
    return u;
  }, [setUser]);

  const register = useCallback(async (email, password) => {
    const u = await authService.register({ email, password });
    setUser(u);
    return u;
  }, [setUser]);

  const logout = useCallback(() => {
    setUser(null);
  }, [setUser]);

  return { user, isAuthenticated, login, register, logout };
}
