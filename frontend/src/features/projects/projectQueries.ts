import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createProject, getProject, listProjects } from '../../api/projects';
import { queryKeys } from '../../api/queryClient';

export function useProjects() {
  return useQuery({
    queryKey: queryKeys.projects(),
    queryFn: ({ signal }) => listProjects(signal),
  });
}

export function useProject(projectId: number) {
  return useQuery({
    queryKey: queryKeys.project(projectId),
    queryFn: ({ signal }) => getProject(projectId, signal),
  });
}

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.projects() }),
  });
}
