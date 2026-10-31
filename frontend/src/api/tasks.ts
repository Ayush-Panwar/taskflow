import { request } from './client';
import type { Page, Task, TaskActivity, TaskInput, TaskListParams, TaskStatus } from './types';

export function listTasks(projectId: number, params: TaskListParams, signal?: AbortSignal): Promise<Page<Task>> {
  return request(`/projects/${projectId}/tasks`, { params, signal });
}

export function createTask(projectId: number, input: TaskInput): Promise<Task> {
  return request(`/projects/${projectId}/tasks`, { method: 'POST', body: input });
}

export function updateTask(taskId: number, input: TaskInput): Promise<Task> {
  return request(`/tasks/${taskId}`, { method: 'PUT', body: input });
}

export function updateTaskStatus(taskId: number, status: TaskStatus): Promise<Task> {
  return request(`/tasks/${taskId}/status`, { method: 'PATCH', body: { status } });
}

export function deleteTask(taskId: number): Promise<void> {
  return request(`/tasks/${taskId}`, { method: 'DELETE' });
}

export function listTaskActivity(taskId: number, signal?: AbortSignal): Promise<TaskActivity[]> {
  return request(`/tasks/${taskId}/activity`, { signal });
}
