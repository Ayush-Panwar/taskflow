import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button } from './Button';

export function Brand() {
  return (
    <span className="brand">
      <img className="brand__mark" src="/favicon.svg" alt="" width="24" height="24" />
      TaskFlow
    </span>
  );
}

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/projects" className="topbar__brand">
          <Brand />
        </Link>
        <div className="topbar__user">
          <span className="topbar__name">{user?.name}</span>
          <Button size="small" variant="ghost" onClick={logout}>
            Sign out
          </Button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  );
}
