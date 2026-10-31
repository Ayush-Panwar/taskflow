import { aSession } from '../test/helpers';
import { clearSession, isExpired, loadSession, saveSession, SESSION_KEY } from './sessionStorage';

describe('session storage', () => {
  beforeEach(() => clearSession());
  afterEach(() => jest.restoreAllMocks());

  it('saves, loads and clears a session', () => {
    const session = aSession();

    saveSession(session);
    expect(loadSession()).toEqual(session);

    clearSession();
    expect(loadSession()).toBeNull();
  });

  it('treats a corrupt stored value as logged out', () => {
    localStorage.setItem(SESSION_KEY, '{not json');

    expect(loadSession()).toBeNull();
  });

  describe('when the browser blocks localStorage', () => {
    beforeEach(() => {
      const blocked = () => {
        throw new Error('Storage is blocked');
      };
      jest.spyOn(Storage.prototype, 'getItem').mockImplementation(blocked);
      jest.spyOn(Storage.prototype, 'setItem').mockImplementation(blocked);
      jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(blocked);
    });

    it('keeps the session in memory, so that requests can still send the token', () => {
      const session = aSession({ accessToken: 'in-memory-token' });

      saveSession(session);

      expect(loadSession()?.accessToken).toBe('in-memory-token');
    });

    it('can still log out', () => {
      saveSession(aSession());

      clearSession();

      expect(loadSession()).toBeNull();
    });
  });

  it('knows when a session has expired', () => {
    expect(isExpired(aSession({ expiresAt: new Date(Date.now() - 1000).toISOString() }))).toBe(true);
    expect(isExpired(aSession({ expiresAt: new Date(Date.now() + 60_000).toISOString() }))).toBe(false);
  });
});
