import type { Task } from '../../api/types';
import { Button } from '../../components/Button';
import { Modal } from '../../components/Modal';
import { FormError } from '../../components/StateMessage';
import { useDeleteTask } from './taskQueries';

interface DeleteTaskModalProps {
  projectId: number;
  task: Task;
  onClose: () => void;
}

export function DeleteTaskModal({ projectId, task, onClose }: DeleteTaskModalProps) {
  const deleteTask = useDeleteTask(projectId);

  return (
    <Modal title="Delete this task?" onClose={onClose}>
      <p className="modal__text">
        <strong>{task.title}</strong> and its history will be permanently deleted.
      </p>
      <FormError message={deleteTask.error?.message ?? null} />
      <div className="modal__actions">
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="danger"
          loading={deleteTask.isPending}
          onClick={() => deleteTask.mutate(task.id, { onSuccess: onClose })}
        >
          Delete task
        </Button>
      </div>
    </Modal>
  );
}
