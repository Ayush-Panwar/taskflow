import type { User } from '../api/types';

export const SESSION_KEY = 'taskflow.session';

export interface Session {
  accessToken: string;
  expiresAt: string;
  user: User;
}

export function loadSession(): Session | null {
  try {
    const stored = localStorage.getItem(SESSION_KEY);
    return stored ? (JSON.parse(stored) as Session) : null;
  } catch {
    return null; // Storage is blocked or the value is corrupt, so treat the user as logged out.
  }
}

export function saveSession(session: Session) {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // Storage is blocked. The login works until the page is reloaded.
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(SESSION_KEY);
  } catch {
    // Nothing was stored, so there is nothing to clear.
  }
}

export function isExpired(session: Session): boolean {
  return new Date(session.expiresAt).getTime() <= Date.now();
}
