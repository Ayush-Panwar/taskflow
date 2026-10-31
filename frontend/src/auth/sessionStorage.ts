import type { User } from '../api/types';

export const SESSION_KEY = 'taskflow.session';

export interface Session {
  accessToken: string;
  expiresAt: string;
  user: User;
}

/** Used only if the browser blocks localStorage, so that a login still works until the page is reloaded. */
let sessionInMemory: Session | null = null;

export function loadSession(): Session | null {
  try {
    const stored = localStorage.getItem(SESSION_KEY);
    return stored ? (JSON.parse(stored) as Session) : null;
  } catch {
    return sessionInMemory; // Storage is blocked, or the stored value is corrupt.
  }
}

export function saveSession(session: Session) {
  sessionInMemory = session;
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // Storage is blocked. The copy in memory is used instead.
  }
}

export function clearSession() {
  sessionInMemory = null;
  try {
    localStorage.removeItem(SESSION_KEY);
  } catch {
    // Storage is blocked, so nothing was stored there.
  }
}

export function isExpired(session: Session): boolean {
  return new Date(session.expiresAt).getTime() <= Date.now();
}
