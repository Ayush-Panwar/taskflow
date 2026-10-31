import { useInfiniteQuery, useMutation, useMutationState, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query';
import { getProjectSummary } from '../../api/projects';
import { queryKeys } from '../../api/queryClient';
import { createTask, deleteTask, listTaskActivity, listTasks, updateTask, updateTaskStatus } from '../../api/tasks';
import type { Task, TaskInput, TaskStatus } from '../../api/types';
import { listUsers } from '../../api/users';
import { useToast } from '../../components/Toast';
import { moveTaskInPages, type TaskPages } from './moveTaskInPages';
import type { BoardFilters } from './useBoardFilters';

const COLUMN_PAGE_SIZE = 20;

/**
 * Each column loads its own pages using the status filter. If the board fetched one mixed page of
 * tasks instead, a column could look short or empty while its tasks sat on a later page.
 */
export function useColumnTasks(projectId: number, status: TaskStatus, filters: BoardFilters) {
  return useInfiniteQuery({
    queryKey: queryKeys.column(projectId, status, filters),
    queryFn: ({ pageParam, signal }) =>
      listTasks(projectId, { ...filters, status, page: pageParam, size: COLUMN_PAGE_SIZE }, signal),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });
}

export function useProjectSummary(projectId: number) {
  return useQuery({
    queryKey: queryKeys.summary(projectId),
    queryFn: ({ signal }) => getProjectSummary(projectId, signal),
  });
}

export function useUsers() {
  return useQuery({
    queryKey: queryKeys.users(),
    queryFn: ({ signal }) => listUsers(signal),
    staleTime: 60_000,
  });
}

export function useTaskActivity(taskId: number) {
  return useQuery({
    queryKey: queryKeys.activity(taskId),
    queryFn: ({ signal }) => listTaskActivity(taskId, signal),
  });
}

/** Refreshes everything the board shows after a task changes. */
function useRefreshBoard(projectId: number) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.tasks(projectId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.summary(projectId) }),
    ]);
}

export function useCreateTask(projectId: number) {
  const refreshBoard = useRefreshBoard(projectId);
  return useMutation({
    mutationFn: (input: TaskInput) => createTask(projectId, input),
    onSuccess: refreshBoard,
  });
}

export function useUpdateTask(projectId: number) {
  const refreshBoard = useRefreshBoard(projectId);
  return useMutation({
    mutationFn: ({ taskId, input }: { taskId: number; input: TaskInput }) => updateTask(taskId, input),
    onSuccess: refreshBoard,
  });
}

export function useDeleteTask(projectId: number) {
  const refreshBoard = useRefreshBoard(projectId);
  return useMutation({
    mutationFn: (taskId: number) => deleteTask(taskId),
    onSuccess: refreshBoard,
  });
}

interface MoveTaskVariables {
  task: Task;
  status: TaskStatus;
}

function moveTaskKey(projectId: number) {
  return ['move-task', projectId];
}

/**
 * Moves a card immediately and saves in the background. If the save fails, the card goes back to
 * where it was and the user sees why.
 */
export function useMoveTask(projectId: number) {
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const refreshBoard = useRefreshBoard(projectId);
  const mutationKey = moveTaskKey(projectId);

  return useMutation({
    mutationKey,
    // Requests run one at a time, in order. Otherwise a card moved twice in quick succession could send
    // two overlapping requests, and the second would fail its version check or the two could arrive reversed.
    scope: { id: `move-task-${projectId}` },
    mutationFn: ({ task, status }: MoveTaskVariables) => updateTaskStatus(task.id, status),
    onMutate: async ({ task, status }) => {
      const tasksKey = queryKeys.tasks(projectId);
      // A refetch already in flight could finish after this and undo the move on screen.
      await queryClient.cancelQueries({ queryKey: tasksKey });
      const previous = queryClient.getQueriesData<TaskPages>({ queryKey: tasksKey });
      const moved: Task = { ...task, status, overdue: status === 'DONE' ? false : task.overdue };
      for (const [key, pages] of previous) {
        if (pages) {
          const columnStatus = columnStatusOf(key);
          queryClient.setQueryData<TaskPages>(key, moveTaskInPages(pages, columnStatus, moved, task.status));
        }
      }
      return { previous };
    },
    onError: (error, _variables, context) => {
      for (const [key, pages] of context?.previous ?? []) {
        queryClient.setQueryData(key, pages);
      }
      showError(`Could not move the task: ${error.message}`);
    },
    onSettled: (_data, _error, { task }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.activity(task.id) });
      // With several moves in flight, refresh after the last one, so no half-finished state comes back.
      if (queryClient.isMutating({ mutationKey }) === 1) {
        return refreshBoard();
      }
      return undefined;
    },
  });
}

function columnStatusOf(key: QueryKey): TaskStatus {
  return key[3] as TaskStatus; // See queryKeys.column.
}

/** The ids of tasks with a move still being saved, so their cards can show it. */
export function usePendingMoves(projectId: number): number[] {
  return useMutationState({
    filters: { mutationKey: moveTaskKey(projectId), status: 'pending' },
    select: (mutation) => (mutation.state.variables as MoveTaskVariables).task.id,
  });
}
