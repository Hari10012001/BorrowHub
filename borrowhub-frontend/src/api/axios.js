import axios from 'axios';

const resolveBaseUrl = () => {
  let url = '';
  if (import.meta.env.VITE_API_BASE_URL) {
    url = String(import.meta.env.VITE_API_BASE_URL).trim();
  } else if (typeof window !== 'undefined' && window.__BORROWHUB_CONFIG__?.API_BASE_URL) {
    url = String(window.__BORROWHUB_CONFIG__.API_BASE_URL).trim();
  }

  if (!url || url === '/api') {
    return '/api';
  }

  // Remove trailing slashes
  url = url.replace(/\/+$/, '');

  // Ensure /api suffix exists since all backend controllers are mapped under /api/*
  if (!url.endsWith('/api')) {
    url += '/api';
  }

  return url;
};

const api = axios.create({
  baseURL: resolveBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('borrowhub_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('borrowhub_token');
      localStorage.removeItem('borrowhub_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
