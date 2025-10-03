import {apiClient, ENDPOINTS} from './apiClient';

function setTokenCookie(token) {
  if (!token) return;
  try {
    document.cookie = `vd_token=${encodeURIComponent(token)}; Path=/; SameSite=Lax`;
  } catch (_) {}
}

export const authService = {
  async login({ email, password }) {
    try {
      const res = await apiClient.post(ENDPOINTS.login, { email, password });
      if (res && res.token) setTokenCookie(res.token);
      if (res && (res.id !== undefined || res.email !== undefined)) {
        return { id: res.id, email: res.email };
      }
      return res;
    } catch (e) {
      return JSON.parse(e.message);
    }
  },
  async register({ email, password }) {
    try {
      const res = await apiClient.post(ENDPOINTS.register, { email, password });
      if (res && res.token) setTokenCookie(res.token);
      if (res && (res.id !== undefined || res.email !== undefined)) {
        return { id: res.id, email: res.email };
      }
      return res;
    } catch (e) {
      return JSON.parse(e.message);
    }
  },
};
