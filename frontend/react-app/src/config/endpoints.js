// Centralized endpoints and configuration
export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
export const WS_NOTIFICATIONS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/notifications';

export const ENDPOINTS = {
  login: `${API_BASE_URL}/auth/login`,
  register: `${API_BASE_URL}/auth/register`,
  documents: `${API_BASE_URL}/documents`,
  search: `${API_BASE_URL}/search`,
};
