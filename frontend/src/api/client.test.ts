import { saveSession } from '../auth/sessionStorage';
import { aSession } from '../test/helpers';
import { ApiError, request, setUnauthorizedHandler, toApiError } from './client';

/** Resolves to the error a failed request rejects with. */
function failureOf(result: Promise<unknown>): Promise<ApiError> {
  return result.then(
    () => {
      throw new Error('Expected the request to fail');
    },
    (error: ApiError) => error,
  );
}

function respondWith(status: number, body: unknown = '') {
  const text = typeof body === 'string' ? body : JSON.stringify(body);
  return jest.fn().mockResolvedValue({ ok: status >= 200 && status < 300, status, text: async () => text });
}

describe('request', () => {
  const onUnauthorized = jest.fn();

  beforeEach(() => {
    onUnauthorized.mockReset();
    setUnauthorizedHandler(onUnauthorized);
  });

  it('sends the saved token and JSON, and returns the parsed response', async () => {
    saveSession(aSession({ accessToken: 'abc' }));
    globalThis.fetch = respondWith(200, { id: 1 });

    const result = await request('/projects', { method: 'POST', body: { name: 'Website' } });

    expect(result).toEqual({ id: 1 });
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/projects',
      expect.objectContaining({
        method: 'POST',
        body: '{"name":"Website"}',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer abc' },
      }),
    );
  });

  it('leaves out query parameters that have no value', async () => {
    globalThis.fetch = respondWith(200, []);

    await request('/projects/1/tasks', { params: { status: 'TODO', priority: undefined, assignee: 0, sort: '' } });

    expect(jest.mocked(globalThis.fetch).mock.calls[0][0]).toBe('/api/projects/1/tasks?status=TODO&assignee=0');
  });

  it('handles an empty 204 response', async () => {
    globalThis.fetch = respondWith(204);

    await expect(request('/tasks/1', { method: 'DELETE' })).resolves.toBeNull();
  });

  it('turns an error response into an ApiError with field messages', async () => {
    globalThis.fetch = respondWith(400, {
      code: 'VALIDATION_FAILED',
      message: 'Request validation failed',
      fieldErrors: [
        { field: 'title', message: 'Title is required' },
        { field: 'title', message: 'A second message for the same field' },
        { field: 'dueDate', message: 'Due date cannot be in the past' },
      ],
    });

    const error = await failureOf(request('/tasks'));

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 400, code: 'VALIDATION_FAILED', message: 'Request validation failed' });
    expect(error.fieldErrors).toEqual({
      title: 'Title is required',
      dueDate: 'Due date cannot be in the past',
    });
  });

  it('copes with an error response that is not JSON, such as an HTML page from a proxy', async () => {
    globalThis.fetch = respondWith(502, '<html>Bad Gateway</html>');

    const error = await failureOf(request('/tasks'));

    expect(error).toMatchObject({ status: 502, message: 'The server is having problems. Please try again shortly.' });
  });

  it('reports a network failure in plain language', async () => {
    globalThis.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch'));

    const error = await failureOf(request('/tasks'));

    expect(error).toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
    expect(error.message).toContain('Could not reach the server');
  });

  it('passes through a deliberate cancellation unchanged', async () => {
    const controller = new AbortController();
    controller.abort();
    const abortError = new DOMException('Aborted', 'AbortError');
    globalThis.fetch = jest.fn().mockRejectedValue(abortError);

    await expect(request('/tasks', { signal: controller.signal })).rejects.toBe(abortError);
  });

  describe('when the server answers 401', () => {
    it('ends the session if a token was sent', async () => {
      saveSession(aSession());
      globalThis.fetch = respondWith(401, { code: 'UNAUTHORIZED', message: 'A valid access token is required' });

      await expect(request('/projects')).rejects.toMatchObject({ status: 401 });

      expect(onUnauthorized).toHaveBeenCalledTimes(1);
    });

    it('does nothing extra for a failed login, where no token was sent', async () => {
      globalThis.fetch = respondWith(401, { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' });

      await expect(request('/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({
        message: 'Invalid email or password',
      });

      expect(onUnauthorized).not.toHaveBeenCalled();
    });

    it('ignores a late answer to a request made with an old token', async () => {
      saveSession(aSession({ accessToken: 'old-token' }));
      globalThis.fetch = jest.fn().mockImplementation(async () => {
        saveSession(aSession({ accessToken: 'new-token' })); // The user logged in again meanwhile.
        return { ok: false, status: 401, text: async () => '{}' };
      });

      await expect(request('/projects')).rejects.toMatchObject({ status: 401 });

      expect(onUnauthorized).not.toHaveBeenCalled();
    });
  });
});

describe('toApiError', () => {
  it('keeps an ApiError and wraps anything else', () => {
    const apiError = new ApiError(404, 'NOT_FOUND', 'Task not found');

    expect(toApiError(apiError)).toBe(apiError);
    expect(toApiError(new Error('boom'))).toMatchObject({ status: 0, code: 'UNKNOWN' });
  });
});
