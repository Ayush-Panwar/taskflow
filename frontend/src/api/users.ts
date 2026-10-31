import { request } from './client';
import type { UserSummary } from './types';

export function listUsers(signal?: AbortSignal): Promise<UserSummary[]> {
  return request('/users', { signal });
}
