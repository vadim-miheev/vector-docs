import {apiClient, ENDPOINTS} from './apiClient';

export const documentsService = {
  async list() {
      const data = await apiClient.get(ENDPOINTS.documents);
      return Array.isArray(data) ? data : [];
  },
  async upload(file) {
      const form = new FormData();
      form.append('file', file);
      return await apiClient.postForm(ENDPOINTS.documents, form);
  },
  async remove(id) {
      await apiClient.del(`${ENDPOINTS.documents}/${encodeURIComponent(id)}`);
      return true;
  },
};
