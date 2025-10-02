import {apiClient, ENDPOINTS} from './apiClient';

export const authService = {
  async login({ email, password }) {
    // Placeholder: call backend if available
    try {
        return await apiClient.post(ENDPOINTS.login, {email, password});
    } catch (e) {
      // Fallback mock user for local-only demo
      return { id: 'local', email };
    }
  },
  async register({ email, password }) {
    try {
        return await apiClient.post(ENDPOINTS.register, {email, password});
    } catch (e) {
      return { id: 'local', email };
    }
  },
};
