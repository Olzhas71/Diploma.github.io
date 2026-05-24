import { api } from './client';
import type { AuthResponse, UserResponse } from '@/types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),

  register: (email: string, password: string, fullName: string, phone?: string) =>
    api.post<AuthResponse>('/auth/register', { email, password, fullName, phone }).then((r) => r.data),

  refresh: (refreshToken: string) =>
    api.post<AuthResponse>('/auth/refresh', { refreshToken }).then((r) => r.data),

  me: () => api.get<UserResponse>('/users/me').then((r) => r.data),
};
