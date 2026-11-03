import {
  defaultDropAnimation,
  DndContext,
  DragOverlay,
  MouseSensor,
  pointerWithin,
  TouchSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
  type DropAnimationKeyframeResolver,
} from '@dnd-kit/core';
import { useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import type { Project, Task, TaskStatus } from '../../api/types';
import { Button } from '../../components/Button';
import { LoadingMessage } from '../../components/Spinner';
import { ErrorState } from '../../components/StateMessage';
import { STATUSES } from '../../lib/taskLabels';
import { useProject } from '../projects/projectQueries';
import { BoardColumn } from './BoardColumn';
import { BoardFiltersBar } from './BoardFiltersBar';
import { DeleteTaskModal } from './DeleteTaskModal';
import { SummaryBar } from './SummaryBar';
import { TaskCard, type TaskCardHandlers } from './TaskCard';
import { TaskFormModal } from './TaskFormModal';
import { useMoveTask, usePendingMoves } from './taskQueries';
import { useBoardFilters } from './useBoardFilters';

function ProjectNotFound() {
  return (
    <ErrorState
      title="Project not found"
      message="It may have been removed, or you may not have access to it."
      action={<Link to="/projects">Back to projects</Link>}
    />
  );
}

export function BoardPage() {
  const projectId = Number(useParams().projectId);

  if (!Number.isInteger(projectId) || projectId <= 0) {
    return <ProjectNotFound />;
  }
  return <ProjectLoader key={projectId} projectId={projectId} />;
}

function ProjectLoader({ projectId }: { projectId: number }) {
  const project = useProject(projectId);

  if (project.isPending) {
    return <LoadingMessage label="Loading project…" />;
  }
  if (project.isError && !project.data) {
    if (project.error instanceof ApiError && project.error.status === 404) {
      return <ProjectNotFound />;
    }
    return (
      <ErrorState
        title="Could not load this project"
        message={project.error.message}
        onRetry={() => project.refetch()}
        retrying={project.isFetching}
      />
    );
  }
  return <Board project={project.data} />;
}

/** What the task form is open for: a new task when there is no task to edit. */
type TaskFormTarget = { task?: Task };

/**
 * By default dnd-kit flies the floating card back to the original card. That's right when the card
 * stays where it was. A moved card has already left for its new column, so flying back would go the
 * wrong way, and it fades out where it was dropped instead.
 */
const fadeOutWhereDropped: DropAnimationKeyframeResolver = ({ transform: { initial } }) => {
  const transform = `translate3d(${initial.x}px, ${initial.y}px, 0)`;
  return [
    { transform, opacity: 1 },
    { transform, opacity: 0 },
  ];
};

function Board({ project }: { project: Project }) {
  const boardFilters = useBoardFilters();
  const moveTask = useMoveTask(project.id);
  const pendingMoves = usePendingMoves(project.id);
  const cardWasMoved = useRef(false);

  const [formTarget, setFormTarget] = useState<TaskFormTarget | null>(null);
  const [taskToDelete, setTaskToDelete] = useState<Task | null>(null);
  const [draggedTask, setDraggedTask] = useState<Task | null>(null);

  // A small movement or a long press starts a drag, so clicks and touch scrolling keep working.
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 250, tolerance: 8 } }),
  );

  const moveTaskMutate = moveTask.mutate;
  const handlers = useMemo<TaskCardHandlers>(
    () => ({
      onEdit: (task) => setFormTarget({ task }),
      onDelete: (task) => setTaskToDelete(task),
      onStatusChange: (task, status) => moveTaskMutate({ task, status }),
    }),
    [moveTaskMutate],
  );

  function handleDragStart(event: DragStartEvent) {
    cardWasMoved.current = false;
    setDraggedTask(event.active.data.current?.task ?? null);
  }

  function handleDragEnd(event: DragEndEvent) {
    setDraggedTask(null);
    const task = event.active.data.current?.task as Task | undefined;
    const status = event.over?.id as TaskStatus | undefined;
    if (task && status && status !== task.status) {
      cardWasMoved.current = true;
      moveTaskMutate({ task, status });
    }
  }

  return (
    <>
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/projects">Projects</Link>
        <span aria-hidden="true"> / </span>
        <span aria-current="page">{project.name}</span>
      </nav>

      <div className="page-header">
        <div>
          <h1 className="page-header__title">{project.name}</h1>
          {project.description && <p className="page-header__description">{project.description}</p>}
        </div>
        <Button variant="primary" onClick={() => setFormTarget({})}>
          New task
        </Button>
      </div>

      <SummaryBar projectId={project.id} />
      <BoardFiltersBar {...boardFilters} />

      <DndContext
        sensors={sensors}
        collisionDetection={pointerWithin}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        onDragCancel={() => setDraggedTask(null)}
      >
        <div className="board">
          {STATUSES.map((status) => (
            <BoardColumn
              key={status}
              projectId={project.id}
              status={status}
              filters={boardFilters.filters}
              isFiltering={boardFilters.isFiltering}
              pendingMoves={pendingMoves}
              handlers={handlers}
            />
          ))}
        </div>
        <DragOverlay
          dropAnimation={{
            keyframes: (parameters) =>
              (cardWasMoved.current ? fadeOutWhereDropped : defaultDropAnimation.keyframes)(parameters),
          }}
        >
          {draggedTask && <TaskCard task={draggedTask} />}
        </DragOverlay>
      </DndContext>

      {formTarget && (
        <TaskFormModal projectId={project.id} task={formTarget.task} onClose={() => setFormTarget(null)} />
      )}
      {taskToDelete && (
        <DeleteTaskModal projectId={project.id} task={taskToDelete} onClose={() => setTaskToDelete(null)} />
      )}
    </>
  );
}
