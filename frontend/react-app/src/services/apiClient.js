import { ENDPOINTS } from '../config/endpoints';

async function request(url, { method = 'GET', headers = {}, body, isForm = false } = {}) {
  const opts = { method, headers: { ...headers }, body: undefined };
  if (body !== undefined) {
    if (isForm) {
      opts.body = body; // FormData
    } else {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
  }
  const res = await fetch(url, opts);
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Request failed: ${res.status}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return res.text();
}

export const apiClient = {
  get: (url) => request(url),
  post: (url, body) => request(url, { method: 'POST', body }),
  postForm: (url, formData) => request(url, { method: 'POST', body: formData, isForm: true }),
  del: (url) => request(url, { method: 'DELETE' }),
};

export { ENDPOINTS };
