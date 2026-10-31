import type { Task, TaskInput, TaskPriority } from '../../api/types';
import type { FieldErrors } from '../../hooks/useForm';
import { todayIso } from '../../lib/dates';

/** Form inputs always hold strings; these are converted to real types when the form is submitted. */
export interface TaskFormValues extends Record<string, string> {
  title: string;
  description: string;
  priority: string;
  dueDate: string;
  assigneeId: string;
}

export const EMPTY_TASK_FORM: TaskFormValues = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: '',
  assigneeId: '',
};

export function taskToFormValues(task: Task): TaskFormValues {
  return {
    title: task.title,
    description: task.description ?? '',
    priority: task.priority,
    dueDate: task.dueDate ?? '',
    assigneeId: task.assignee ? String(task.assignee.id) : '',
  };
}

export function formValuesToInput(values: TaskFormValues): TaskInput {
  return {
    title: values.title.trim(),
    description: values.description.trim() || null,
    priority: values.priority as TaskPriority,
    dueDate: values.dueDate || null,
    assigneeId: values.assigneeId ? Number(values.assigneeId) : null,
  };
}

export function validateTaskForm(values: TaskFormValues, isEditing: boolean): FieldErrors<TaskFormValues> {
  const title = values.title.trim();
  const errors: FieldErrors<TaskFormValues> = {};

  if (!title) {
    errors.title = 'Title is required';
  } else if (title.length > 120) {
    errors.title = 'Title must be at most 120 characters';
  }
  if (values.description.length > 2000) {
    errors.description = 'Description must be at most 2000 characters';
  }
  // On create only: an overdue task must still be editable without changing its date.
  if (!isEditing && values.dueDate && values.dueDate < todayIso()) {
    errors.dueDate = 'Due date cannot be in the past';
  }
  return errors;
}
