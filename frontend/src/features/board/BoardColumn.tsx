import { useDraggable, useDroppable } from '@dnd-kit/core';
import type { Task, TaskStatus } from '../../api/types';
import { Button } from '../../components/Button';
import { EmptyState, ErrorState } from '../../components/StateMessage';
import { STATUS_LABELS } from '../../lib/taskLabels';
import { useColumnTasks } from './taskQueries';
import { TaskCard, type TaskCardHandlers } from './TaskCard';
import type { BoardFilters } from './useBoardFilters';

interface DraggableTaskCardProps {
  task: Task;
  handlers: TaskCardHandlers;
  saving: boolean;
}

function DraggableTaskCard({ task, handlers, saving }: DraggableTaskCardProps) {
  const { setNodeRef, listeners, isDragging } = useDraggable({ id: task.id, data: { task } });
  return (
    <li ref={setNodeRef} {...listeners} className={`task-list__item ${isDragging ? 'task-list__item--dragging' : ''}`}>
      <TaskCard task={task} handlers={handlers} saving={saving} />
    </li>
  );
}

function ColumnSkeleton() {
  return (
    <div className="column-skeleton" role="status">
      <span className="visually-hidden">Loading tasks</span>
      <div className="skeleton skeleton--card" />
      <div className="skeleton skeleton--card" />
    </div>
  );
}

interface BoardColumnProps {
  projectId: number;
  status: TaskStatus;
  filters: BoardFilters;
  isFiltering: boolean;
  pendingMoves: number[];
  handlers: TaskCardHandlers;
}

export function BoardColumn({ projectId, status, filters, isFiltering, pendingMoves, handlers }: BoardColumnProps) {
  const tasks = useColumnTasks(projectId, status, filters);
  const { setNodeRef, isOver } = useDroppable({ id: status });

  const loadedTasks = tasks.data?.pages.flatMap((page) => page.content) ?? [];
  const total = tasks.data?.pages[0]?.totalElements;
  const remaining = (total ?? 0) - loadedTasks.length;

  return (
    <section
      ref={setNodeRef}
      className={`column column--${status.toLowerCase().replace('_', '-')} ${isOver ? 'column--drop-target' : ''}`}
      aria-label={STATUS_LABELS[status]}
    >
      <header className="column__header">
        <h2 className="column__title">{STATUS_LABELS[status]}</h2>
        {total !== undefined && <span className="column__count">{total}</span>}
      </header>

      {tasks.isPending && <ColumnSkeleton />}

      {tasks.isError && !tasks.data && (
        <ErrorState
          title="Could not load tasks"
          message={tasks.error.message}
          onRetry={() => tasks.refetch()}
          retrying={tasks.isFetching}
        />
      )}

      {tasks.data && loadedTasks.length === 0 && (
        <EmptyState title={isFiltering ? 'No tasks match these filters' : 'No tasks here'} />
      )}

      {loadedTasks.length > 0 && (
        <ul className="task-list">
          {loadedTasks.map((task) => (
            <DraggableTaskCard key={task.id} task={task} handlers={handlers} saving={pendingMoves.includes(task.id)} />
          ))}
        </ul>
      )}

      {tasks.isRefetchError && tasks.data && (
        <p className="column__notice" role="alert">
          This list may be out of date.{' '}
          <button type="button" className="link-button" onClick={() => tasks.refetch()}>
            Refresh
          </button>
        </p>
      )}

      {tasks.isFetchNextPageError && (
        <p className="column__notice" role="alert">
          Could not load more tasks.
        </p>
      )}

      {tasks.hasNextPage && (
        <Button
          size="small"
          className="column__more"
          loading={tasks.isFetchingNextPage}
          onClick={() => tasks.fetchNextPage()}
        >
          {tasks.isFetchNextPageError ? 'Try again' : `Load more (${remaining} left)`}
        </Button>
      )}
    </section>
  );
}
