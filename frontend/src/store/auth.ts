import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import axios from 'axios';
import type { Role } from '@/types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: number | null;
  email: string | null;
  role: Role | null;

  setSession: (data: {
    accessToken: string;
    refreshToken: string;
    userId: number;
    email: string;
    role: Role;
  }) => void;
  logout: () => void;
  refresh: () => Promise<string | null>;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      email: null,
      role: null,

      setSession: ({ accessToken, refreshToken, userId, email, role }) =>
        set({ accessToken, refreshToken, userId, email, role }),

      logout: () =>
        set({ accessToken: null, refreshToken: null, userId: null, email: null, role: null }),

      refresh: async () => {
        const rt = get().refreshToken;
        if (!rt) return null;
        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken: rt });
          set({
            accessToken: res.data.accessToken,
            refreshToken: res.data.refreshToken,
            userId: res.data.userId,
            email: res.data.email,
            role: res.data.role,
          });
          return res.data.accessToken;
        } catch {
          get().logout();
          return null;
        }
      },

      isAuthenticated: () => !!get().accessToken,
    }),
    { name: 'parking-auth' }
  )
);
