import { QueryClient } from '@tanstack/react-query';
import { ApiError } from './client';

/** A client error will fail the same way every time, so only retry network and server problems. */
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 2;
}

/**
 * networkMode 'always': by default, requests silently pause while the browser reports being offline,
 * which would leave spinners running with no explanation. Trying anyway gives a clear error instead.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: shouldRetry,
        staleTime: 10_000,
        networkMode: 'always',
      },
      mutations: {
        networkMode: 'always',
      },
    },
  });
}

/**
 * Keys are hierarchical, so invalidating a prefix refreshes everything below it.
 * For example, invalidating tasks(1) refreshes all three columns of project 1.
 */
export const queryKeys = {
  projects: () => ['projects'] as const,
  project: (projectId: number) => ['projects', projectId] as const,
  summary: (projectId: number) => ['projects', projectId, 'summary'] as const,
  tasks: (projectId: number) => ['projects', projectId, 'tasks'] as const,
  column: (projectId: number, status: string, filters: object) =>
    ['projects', projectId, 'tasks', status, filters] as const,
  activity: (taskId: number) => ['tasks', taskId, 'activity'] as const,
  users: () => ['users'] as const,
};
