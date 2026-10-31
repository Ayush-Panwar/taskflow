import type { Task } from '../../api/types';
import { Button } from '../../components/Button';
import { SelectField, TextAreaField, TextField } from '../../components/FormFields';
import { Modal } from '../../components/Modal';
import { FormError } from '../../components/StateMessage';
import { useForm } from '../../hooks/useForm';
import { todayIso } from '../../lib/dates';
import { PRIORITIES, PRIORITY_LABELS } from '../../lib/taskLabels';
import { EMPTY_TASK_FORM, formValuesToInput, taskToFormValues, validateTaskForm } from './taskForm';
import { TaskHistory } from './TaskHistory';
import { useCreateTask, useUpdateTask, useUsers } from './taskQueries';

interface TaskFormModalProps {
  projectId: number;
  /** The task to edit. Leave out to create a new one. */
  task?: Task;
  onClose: () => void;
}

export function TaskFormModal({ projectId, task, onClose }: TaskFormModalProps) {
  const isEditing = task !== undefined;
  const users = useUsers();
  const createTask = useCreateTask(projectId);
  const updateTask = useUpdateTask(projectId);

  const form = useForm({
    initialValues: task ? taskToFormValues(task) : EMPTY_TASK_FORM,
    validate: (values) => validateTaskForm(values, isEditing),
    onSubmit: async (values) => {
      const input = formValuesToInput(values);
      if (task) {
        await updateTask.mutateAsync({ taskId: task.id, input });
      } else {
        await createTask.mutateAsync(input);
      }
      onClose();
    },
  });

  // Keep the current assignee selectable even if the list of people failed to load.
  const currentAssignee = task?.assignee;
  const assigneeMissingFromList = currentAssignee && !users.data?.some((user) => user.id === currentAssignee.id);

  return (
    <Modal title={isEditing ? 'Edit task' : 'New task'} onClose={onClose}>
      <form onSubmit={form.handleSubmit} noValidate>
        <TextField label="Title" data-autofocus {...form.field('title')} />
        <TextAreaField label="Description" rows={3} {...form.field('description')} />

        <div className="form-row">
          <SelectField label="Priority" {...form.field('priority')}>
            {PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>
                {PRIORITY_LABELS[priority]}
              </option>
            ))}
          </SelectField>
          <TextField
            label="Due date"
            type="date"
            // Only new tasks are limited: an overdue task keeps its date while being edited.
            min={isEditing ? undefined : todayIso()}
            {...form.field('dueDate')}
          />
        </div>

        <SelectField
          label="Assignee"
          disabled={users.isPending}
          hint={
            users.isError ? (
              <>
                Could not load people.{' '}
                <button type="button" className="link-button" onClick={() => users.refetch()}>
                  Retry
                </button>
              </>
            ) : undefined
          }
          {...form.field('assigneeId')}
        >
          <option value="">{users.isPending ? 'Loading people…' : 'Unassigned'}</option>
          {assigneeMissingFromList && <option value={currentAssignee.id}>{currentAssignee.name}</option>}
          {users.data?.map((user) => (
            <option key={user.id} value={user.id}>
              {user.name}
            </option>
          ))}
        </SelectField>

        <FormError message={form.formError} />

        <div className="modal__actions">
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="primary" loading={form.submitting}>
            {isEditing ? 'Save changes' : 'Create task'}
          </Button>
        </div>
      </form>

      {task && <TaskHistory taskId={task.id} />}
    </Modal>
  );
}
