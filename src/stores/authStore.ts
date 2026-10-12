import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AdminUser | null;
  setSession: (accessToken: string, refreshToken: string, user: AdminUser) => void;
  clearSession: () => void;
}

/**
 * Admin session store persisted to localStorage (zustand persist).
 * The refresh token lives here too so the API client can rotate it once
 * without re-prompting the user.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (accessToken, refreshToken, user) => set({ accessToken, refreshToken, user }),
      clearSession: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    {
      name: 'maison-admin-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
