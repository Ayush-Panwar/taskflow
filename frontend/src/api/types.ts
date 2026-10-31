export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: number;
  name: string;
  email: string;
}

/** What other people can see about a user. */
export interface UserSummary {
  id: number;
  name: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
}

export interface ProjectInput {
  name: string;
  description: string | null;
}

export interface ProjectSummary {
  projectId: number;
  counts: Record<TaskStatus, number>;
  total: number;
  overdue: number;
}

export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  /** A calendar date, yyyy-MM-dd. */
  dueDate: string | null;
  overdue: boolean;
  assignee: UserSummary | null;
  projectId: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskInput {
  title: string;
  description: string | null;
  priority: TaskPriority;
  dueDate: string | null;
  assigneeId: number | null;
}

export interface TaskListParams {
  status?: TaskStatus;
  priority?: TaskPriority;
  assignee?: number;
  sort?: string;
  page?: number;
  size?: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface TaskActivity {
  id: number;
  actor: UserSummary;
  fromStatus: TaskStatus;
  toStatus: TaskStatus;
  occurredAt: string;
}
