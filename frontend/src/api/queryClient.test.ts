import { onlineManager } from '@tanstack/react-query';
import { ApiError } from './client';
import { createQueryClient } from './queryClient';

describe('when the browser reports being offline', () => {
  const failure = new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server.');

  beforeEach(() => onlineManager.setOnline(false));
  afterEach(() => onlineManager.setOnline(true));

  it('still tries a query, so the user sees an error rather than a spinner forever', async () => {
    const queryFn = jest.fn().mockRejectedValue(failure);

    await expect(createQueryClient().fetchQuery({ queryKey: ['x'], queryFn, retry: false })).rejects.toBe(failure);

    expect(queryFn).toHaveBeenCalledTimes(1);
  });

  it('still tries a mutation, for the same reason', async () => {
    const mutationFn = jest.fn().mockRejectedValue(failure);
    const client = createQueryClient();

    const mutation = client.getMutationCache().build(client, client.defaultMutationOptions({ mutationFn }));

    await expect(mutation.execute(undefined)).rejects.toBe(failure);
    expect(mutationFn).toHaveBeenCalledTimes(1);
  });
});

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
