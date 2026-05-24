import axios from 'axios';
import { useAuthStore } from '@/store/auth';

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        if (!refreshing) refreshing = useAuthStore.getState().refresh();
        const newToken = await refreshing;
        refreshing = null;
        if (newToken) {
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
        }
      } catch {
        refreshing = null;
        useAuthStore.getState().logout();
      }
    }
    return Promise.reject(error);
  }
);
