import type { MouseEvent, TouchEvent } from 'react';
import type { Task, TaskStatus } from '../../api/types';
import { Button } from '../../components/Button';
import { formatDueDate } from '../../lib/dates';
import { PRIORITY_LABELS, STATUS_LABELS, STATUSES } from '../../lib/taskLabels';

export interface TaskCardHandlers {
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
  onStatusChange: (task: Task, status: TaskStatus) => void;
}

interface TaskCardProps {
  task: Task;
  /** Leave out to show the card without any controls, as in the copy that follows the cursor during a drag. */
  handlers?: TaskCardHandlers;
  /** A status change for this task is still being saved. */
  saving?: boolean;
}

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/);
  return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

/** Pressing a control on the card must use that control, not start dragging the card. */
function stopDrag(event: MouseEvent | TouchEvent) {
  event.stopPropagation();
}

export function TaskCard({ task, handlers, saving = false }: TaskCardProps) {
  const classes = ['task-card', task.overdue ? 'task-card--overdue' : '', saving ? 'task-card--saving' : '']
    .filter(Boolean)
    .join(' ');

  return (
    <article className={classes} aria-busy={saving}>
      <div className="task-card__badges">
        <span className={`badge badge--${task.priority.toLowerCase()}`}>{PRIORITY_LABELS[task.priority]}</span>
        {task.overdue && <span className="badge badge--overdue">Overdue</span>}
        {saving && <span className="task-card__saving">Saving…</span>}
      </div>

      <h3 className="task-card__title">
        {handlers ? (
          <button type="button" className="task-card__title-button" onClick={() => handlers.onEdit(task)}>
            {task.title}
          </button>
        ) : (
          task.title
        )}
      </h3>

      {task.description && <p className="task-card__description">{task.description}</p>}

      <div className="task-card__meta">
        <span className={task.overdue ? 'task-card__due task-card__due--overdue' : 'task-card__due'}>
          {task.dueDate ? `Due ${formatDueDate(task.dueDate)}` : 'No due date'}
        </span>
        {task.assignee ? (
          <span className="assignee" title={task.assignee.email}>
            <span className="assignee__avatar" aria-hidden="true">
              {initialsOf(task.assignee.name)}
            </span>
            {task.assignee.name}
          </span>
        ) : (
          <span className="assignee assignee--none">Unassigned</span>
        )}
      </div>

      {handlers && (
        <div className="task-card__actions" onMouseDown={stopDrag} onTouchStart={stopDrag}>
          <select
            className="input input--compact"
            aria-label={`Status of ${task.title}`}
            value={task.status}
            onChange={(event) => handlers.onStatusChange(task, event.target.value as TaskStatus)}
          >
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
          <Button size="small" variant="ghost" aria-label={`Edit ${task.title}`} onClick={() => handlers.onEdit(task)}>
            Edit
          </Button>
          <Button size="small" variant="ghost" aria-label={`Delete ${task.title}`} onClick={() => handlers.onDelete(task)}>
            Delete
          </Button>
        </div>
      )}
    </article>
  );
}
