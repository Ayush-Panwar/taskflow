import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/**
 * Sends logged-out visitors to the login page, remembering where they were going. Someone who chose
 * to sign out is the exception: the next person to log in may be a different user.
 */
export function ProtectedRoute() {
  const { user, signedOut } = useAuth();
  const location = useLocation();

  if (!user) {
    const state = signedOut ? undefined : { from: location.pathname + location.search };
    return <Navigate to="/login" replace state={state} />;
  }
  return <Outlet />;
}

/** The login and register pages. Once logged in, this sends the user on to where they were going. */
export function PublicOnlyRoute() {
  const { user } = useAuth();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  if (user) {
    return <Navigate to={from ?? '/projects'} replace />;
  }
  return <Outlet />;
}
