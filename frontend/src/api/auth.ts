import { request } from './client';
import type { AuthResponse, User } from './types';

export function login(email: string, password: string): Promise<AuthResponse> {
  return request('/auth/login', { method: 'POST', body: { email, password } });
}

export function register(name: string, email: string, password: string): Promise<User> {
  return request('/auth/register', { method: 'POST', body: { name, email, password } });
}
