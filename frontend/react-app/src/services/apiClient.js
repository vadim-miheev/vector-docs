import { ENDPOINTS } from '../config/endpoints';

function getTokenFromCookie() {
  if (typeof document === 'undefined') return null;
  const m = document.cookie.match(/(?:^|;\s*)vd_token=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}

async function request(url, { method = 'GET', headers = {}, body, isForm = false } = {}) {
  const opts = { method, headers: { ...headers }, body: undefined };

  // Attach Authorization header from cookie if exists
  const token = getTokenFromCookie();
  if (token) {
    opts.headers['Authorization'] = `Bearer ${token}`;
  }

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
    if (res.status === 401) {
        throw new Error('Unauthorized');
    }
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
