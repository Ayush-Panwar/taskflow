import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import type { Page, Task, User } from '../api/types';
import { AuthProvider } from '../auth/AuthContext';
import { saveSession, type Session } from '../auth/sessionStorage';
import { ToastProvider } from '../components/Toast';

export const asha: User = { id: 1, name: 'Asha Rao', email: 'asha@example.com' };
export const ben: User = { id: 2, name: 'Ben Lee', email: 'ben@example.com' };

export function aSession(overrides: Partial<Session> = {}): Session {
  return {
    accessToken: 'test-token',
    expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    user: asha,
    ...overrides,
  };
}

export function aTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 100,
    title: 'Write the report',
    description: null,
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: null,
    overdue: false,
    assignee: null,
    projectId: 1,
    createdAt: '2026-01-01T10:00:00Z',
    updatedAt: '2026-01-01T10:00:00Z',
    ...overrides,
  };
}

export function aPage<T>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    hasNext: false,
    ...overrides,
  };
}

interface RenderOptions {
  route?: string;
  session?: Session;
}

/** Renders with the same providers as the real app, but without retries, so failures show up at once. */
export function renderWithApp(ui: ReactElement, { route = '/', session }: RenderOptions = {}) {
  if (session) {
    saveSession(session);
  }
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const view = render(
    <MemoryRouter initialEntries={[route]}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <ToastProvider>{ui}</ToastProvider>
        </AuthProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
  return { ...view, queryClient, user: userEvent.setup() };
}
