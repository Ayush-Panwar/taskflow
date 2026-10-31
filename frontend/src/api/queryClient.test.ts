import { ApiError } from './client';
import { createQueryClient } from './queryClient';

describe('retry policy', () => {
  const retry = createQueryClient().getDefaultOptions().queries?.retry as (count: number, error: unknown) => boolean;

  it('never retries a client error, which would only fail again', () => {
    expect(retry(0, new ApiError(404, 'NOT_FOUND', 'Not found'))).toBe(false);
    expect(retry(0, new ApiError(401, 'UNAUTHORIZED', 'Please sign in'))).toBe(false);
  });

  it('retries network and server errors twice', () => {
    const networkError = new ApiError(0, 'NETWORK_ERROR', 'Offline');
    const serverError = new ApiError(503, 'UNKNOWN', 'Down');

    expect(retry(0, networkError)).toBe(true);
    expect(retry(1, serverError)).toBe(true);
    expect(retry(2, serverError)).toBe(false);
  });
});
