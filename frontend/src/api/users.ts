import { request } from './client';
import type { User } from './types';

export function listUsers(signal?: AbortSignal): Promise<User[]> {
  return request('/users', { signal });
}
