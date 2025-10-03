import {apiClient, ENDPOINTS} from './apiClient';

export const searchService = {
    async search(query) {
        const q = String(query || '').trim();
        if (!q) return [];
        const data = await apiClient.post(ENDPOINTS.search, {query: q});
        return Array.isArray(data) ? data : [];
    },
};
