import { apiClient, ENDPOINTS } from './apiClient';

function localKey(userId) {
  return `vd_docs_${userId}`;
}

function loadLocal(userId) {
  try {
    const raw = localStorage.getItem(localKey(userId));
    return raw ? JSON.parse(raw) : [];
  } catch (_) {
    return [];
  }
}

function saveLocal(userId, list) {
  try {
    localStorage.setItem(localKey(userId), JSON.stringify(list));
  } catch (_) {}
}

export const documentsService = {
  async list(userId) {
    try {
      const data = await apiClient.get(ENDPOINTS.documents);
      return Array.isArray(data) ? data : [];
    } catch (e) {
      return loadLocal(userId);
    }
  },
  async upload(userId, file) {
    try {
      const form = new FormData();
      form.append('file', file);
      const data = await apiClient.postForm(ENDPOINTS.documents, form);
      return data;
    } catch (e) {
      const newItem = { id: Date.now().toString(), name: file.name, size: file.size };
      const list = loadLocal(userId);
      const updated = [newItem, ...list];
      saveLocal(userId, updated);
      return newItem;
    }
  },
  async remove(userId, id) {
    try {
      await apiClient.del(`${ENDPOINTS.documents}/${encodeURIComponent(id)}`);
      return true;
    } catch (e) {
      const list = loadLocal(userId);
      const updated = list.filter((x) => x.id !== id);
      saveLocal(userId, updated);
      return true;
    }
  },
};
