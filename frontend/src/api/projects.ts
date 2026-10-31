import { request } from './client';
import type { Project, ProjectInput, ProjectSummary } from './types';

export function listProjects(signal?: AbortSignal): Promise<Project[]> {
  return request('/projects', { signal });
}

export function getProject(projectId: number, signal?: AbortSignal): Promise<Project> {
  return request(`/projects/${projectId}`, { signal });
}

export function createProject(input: ProjectInput): Promise<Project> {
  return request('/projects', { method: 'POST', body: input });
}

export function getProjectSummary(projectId: number, signal?: AbortSignal): Promise<ProjectSummary> {
  return request(`/projects/${projectId}/summary`, { signal });
}
