import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { setUnauthorizedHandler } from '../api/client';
import type { User } from '../api/types';
import { clearSession, isExpired, loadSession, saveSession, SESSION_KEY, type Session } from './sessionStorage';

interface AuthContextValue {
  user: User | null;
  /** The user was logged out because their session ended, so the login page should say why. */
  sessionExpired: boolean;
  /** The user chose to sign out, so the next login should not return to the page they were on. */
  signedOut: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// setTimeout overflows and fires immediately for delays longer than this (about 24 days).
const MAX_TIMEOUT_MS = 2 ** 31 - 1;

interface AuthState {
  session: Session | null;
  /** Why there is no session, if it was ended rather than never started. */
  endedBy: 'expiry' | 'user' | null;
}

function readInitialState(): AuthState {
  const session = loadSession();
  if (session && isExpired(session)) {
    return { session: null, endedBy: 'expiry' };
  }
  return { session, endedBy: null };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [{ session, endedBy }, setState] = useState<AuthState>(readInitialState);

  const endSession = useCallback(
    (reason: 'expiry' | 'user') => {
      clearSession();
      queryClient.clear(); // Otherwise the next person to log in could see this user's cached data.
      setState({ session: null, endedBy: reason });
    },
    [queryClient],
  );

  // A token found to be expired on page load is still in storage until now.
  useEffect(() => {
    if (!session) {
      clearSession();
    }
  }, [session]);

  // The server rejected the token, for example because it expired or the secret changed.
  useEffect(() => {
    setUnauthorizedHandler(() => endSession('expiry'));
    return () => setUnauthorizedHandler(() => {});
  }, [endSession]);

  // Log out at the moment the token expires, rather than on the next failed request.
  useEffect(() => {
    if (!session) {
      return undefined;
    }
    const delay = new Date(session.expiresAt).getTime() - Date.now();
    const timer = setTimeout(() => endSession('expiry'), Math.min(delay, MAX_TIMEOUT_MS));
    return () => clearTimeout(timer);
  }, [session, endSession]);

  // Keep other open tabs in step when the user logs in or out in this one.
  useEffect(() => {
    function handleStorage(event: StorageEvent) {
      // The key is null when another tab cleared all of localStorage.
      if (event.key === SESSION_KEY || event.key === null) {
        queryClient.clear();
        setState(readInitialState());
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, [queryClient]);

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await authApi.login(email, password);
      const newSession: Session = {
        accessToken: response.accessToken,
        expiresAt: response.expiresAt,
        user: response.user,
      };
      saveSession(newSession);
      queryClient.clear();
      setState({ session: newSession, endedBy: null });
    },
    [queryClient],
  );

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      await authApi.register(name, email, password);
      await login(email, password);
    },
    [login],
  );

  const logout = useCallback(() => endSession('user'), [endSession]);

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      sessionExpired: endedBy === 'expiry',
      signedOut: endedBy === 'user',
      login,
      register,
      logout,
    }),
    [session, endedBy, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
