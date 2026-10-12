import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { useAuthStore } from '@/stores/authStore';

/**
 * Route guard for the admin area: without an ADMIN session the user is sent to
 * /admin/login. A valid CUSTOMER token is not enough — a customer must not be
 * able to render admin pages (the API would reject them with 403 anyway). The
 * token may be expired — {@code lib/adminApi} refreshes it transparently on
 * the first 401.
 */
export default function RequireAdmin({ children }: { children: ReactNode }) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);

  if (!accessToken || !user?.roles.includes('ADMIN')) {
    return <Navigate to="/admin/login" replace />;
  }

  return <>{children}</>;
}
