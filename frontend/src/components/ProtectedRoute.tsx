import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import type { Role } from '@/types';

interface Props {
  children: React.ReactNode;
  roles?: Role[];
}

export function ProtectedRoute({ children, roles }: Props) {
  const location = useLocation();
  const { accessToken, role } = useAuthStore();
  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && (!role || !roles.includes(role))) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
