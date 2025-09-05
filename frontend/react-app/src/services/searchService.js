import { apiClient, ENDPOINTS } from './apiClient';

export const searchService = {
  async search(query) {
    const q = String(query || '').trim();
    if (!q) return [];
    try {
      const data = await apiClient.post(ENDPOINTS.search, { query: q });
      return Array.isArray(data) ? data : [];
    } catch (e) {
      // Fallback: simple local client-side search across names stored for any user
      const keys = Object.keys(localStorage).filter((k) => k.startsWith('vd_docs_'));
      const results = [];
      keys.forEach((k) => {
        try {
          const arr = JSON.parse(localStorage.getItem(k) || '[]');
          arr.forEach((doc) => {
            if ((doc.name || '').toLowerCase().includes(q.toLowerCase())) {
              results.push({ ...doc, _source: k });
            }
          });
        } catch (_) {}
      });
      return results;
    }
  },
};
