import { apiClient, ENDPOINTS } from './apiClient';

export const authService = {
  async login({ email, password }) {
    // Placeholder: call backend if available
    try {
      const data = await apiClient.post(ENDPOINTS.login, { email, password });
      return data;
    } catch (e) {
      // Fallback mock user for local-only demo
      return { id: 'local', email };
    }
  },
  async register({ email, password }) {
    try {
      const data = await apiClient.post(ENDPOINTS.register, { email, password });
      return data;
    } catch (e) {
      return { id: 'local', email };
    }
  },
};
