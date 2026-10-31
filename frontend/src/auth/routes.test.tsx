import { act, screen, waitFor } from '@testing-library/react';
import { Route, Routes, useLocation } from 'react-router-dom';
import * as authApi from '../api/auth';
import { request } from '../api/client';
import * as projectsApi from '../api/projects';
import { useAuth } from './AuthContext';
import { ProtectedRoute, PublicOnlyRoute } from './routes';
import { LoginPage } from '../features/auth/LoginPage';
import { asha, aSession, renderWithApp } from '../test/helpers';
import { loadSession, saveSession } from './sessionStorage';

jest.mock('../api/auth');
jest.mock('../api/projects');

function PrivatePage() {
  const { logout } = useAuth();
  const location = useLocation();
  return (
    <>
      <p>
        Private page at {location.pathname}
        {location.search}
      </p>
      <button onClick={logout}>Sign out</button>
    </>
  );
}

function TestRoutes() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route path="/projects" element={<PrivatePage />} />
        <Route path="/projects/:id" element={<PrivatePage />} />
      </Route>
    </Routes>
  );
}

const authResponse = {
  accessToken: 'new-token',
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  user: asha,
};

describe('protected routes', () => {
  beforeEach(() => {
    jest.mocked(authApi.login).mockResolvedValue(authResponse);
  });

  it('sends a logged-out visitor to the login page, with no expiry message', () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7' });

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByText(/session has expired/i)).not.toBeInTheDocument();
  });

  it('shows the page to a logged-in user', () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });

    expect(screen.getByText('Private page at /projects/7')).toBeInTheDocument();
  });

  it('keeps a logged-in user away from the login page', () => {
    renderWithApp(<TestRoutes />, { route: '/login', session: aSession() });

    expect(screen.getByText('Private page at /projects')).toBeInTheDocument();
  });

  it('after logging in, returns to the exact page the visitor asked for', async () => {
    const { user } = renderWithApp(<TestRoutes />, { route: '/projects/7?priority=HIGH' });

    await user.type(screen.getByLabelText('Email'), 'asha@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Private page at /projects/7?priority=HIGH')).toBeInTheDocument();
  });

  it('after a deliberate sign-out, the next login does not return to the previous page', async () => {
    const { user } = renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });

    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByText(/session has expired/i)).not.toBeInTheDocument();
    expect(loadSession()).toBeNull();

    await user.type(screen.getByLabelText('Email'), 'ben@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Private page at /projects')).toBeInTheDocument();
  });
});

describe('session expiry', () => {
  it('treats a stored token that has already expired as logged out, and says why', async () => {
    renderWithApp(<TestRoutes />, {
      route: '/projects/7',
      session: aSession({ expiresAt: new Date(Date.now() - 1000).toISOString() }),
    });

    expect(screen.getByText(/session has expired/i)).toBeInTheDocument();
    await waitFor(() => expect(loadSession()).toBeNull());
  });

  it('logs out at the moment the token expires', async () => {
    jest.useFakeTimers();
    try {
      renderWithApp(<TestRoutes />, {
        route: '/projects/7',
        session: aSession({ expiresAt: new Date(Date.now() + 5000).toISOString() }),
      });
      expect(screen.getByText('Private page at /projects/7')).toBeInTheDocument();

      await act(() => jest.advanceTimersByTimeAsync(5001));

      expect(screen.getByText(/session has expired/i)).toBeInTheDocument();
      expect(loadSession()).toBeNull();
    } finally {
      jest.useRealTimers();
    }
  });

  it('logs out cleanly when the server rejects the token', async () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });
    globalThis.fetch = jest.fn().mockResolvedValue({ ok: false, status: 401, text: async () => '{}' });

    await act(() => expect(request('/projects')).rejects.toMatchObject({ status: 401 }));

    expect(await screen.findByText(/session has expired/i)).toBeInTheDocument();
    expect(loadSession()).toBeNull();
  });

  it('clears cached data so the next user cannot see it', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([]);
    const { queryClient, user } = renderWithApp(<TestRoutes />, { route: '/projects', session: aSession() });
    queryClient.setQueryData(['projects'], [{ id: 1, name: 'Secret' }]);

    await user.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(queryClient.getQueryData(['projects'])).toBeUndefined();
  });
});

describe('several open tabs', () => {
  function fireStorageEvent(key: string | null) {
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key }));
    });
  }

  it('logs this tab out when another tab signs out', async () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });
    localStorage.clear();

    fireStorageEvent('taskflow.session');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('logs this tab in when another tab logs in', async () => {
    renderWithApp(<TestRoutes />, { route: '/login' });
    saveSession(aSession());

    fireStorageEvent('taskflow.session');

    expect(await screen.findByText('Private page at /projects')).toBeInTheDocument();
  });

  it('does not accept an expired session from another tab', async () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });
    saveSession(aSession({ expiresAt: new Date(Date.now() - 1000).toISOString() }));

    fireStorageEvent('taskflow.session');

    expect(await screen.findByText(/session has expired/i)).toBeInTheDocument();
  });

  it('notices when another tab clears all storage', async () => {
    renderWithApp(<TestRoutes />, { route: '/projects/7', session: aSession() });
    localStorage.clear();

    fireStorageEvent(null);

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });
});
