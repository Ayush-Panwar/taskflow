import { fireEvent, screen, waitFor } from '@testing-library/react';
import { ApiError } from '../../api/client';
import * as tasksApi from '../../api/tasks';
import * as usersApi from '../../api/users';
import { asha, aTask, ben, renderWithApp } from '../../test/helpers';
import { TaskFormModal } from './TaskFormModal';

jest.mock('../../api/tasks');
jest.mock('../../api/users');

const onClose = jest.fn();

beforeEach(() => {
  onClose.mockReset();
  jest.mocked(usersApi.listUsers).mockResolvedValue([asha, ben]);
  jest.mocked(tasksApi.listTaskActivity).mockResolvedValue([]);
});

function setDueDate(value: string) {
  fireEvent.change(screen.getByLabelText('Due date'), { target: { value } });
}

describe('creating a task', () => {
  it('needs a title before anything is sent', async () => {
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    await user.click(screen.getByRole('button', { name: 'Create task' }));

    expect(screen.getByText('Title is required')).toBeInTheDocument();
    expect(tasksApi.createTask).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('limits the title to 120 characters', async () => {
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'x'.repeat(121) } });
    await user.click(screen.getByRole('button', { name: 'Create task' }));

    expect(screen.getByText('Title must be at most 120 characters')).toBeInTheDocument();
  });

  it('rejects a due date in the past', async () => {
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    await user.type(screen.getByLabelText('Title'), 'Report');
    setDueDate('2020-01-01');
    await user.click(screen.getByRole('button', { name: 'Create task' }));

    expect(screen.getByText('Due date cannot be in the past')).toBeInTheDocument();
    expect(tasksApi.createTask).not.toHaveBeenCalled();
  });

  it('sends the values in the shape the API expects, then closes', async () => {
    jest.mocked(tasksApi.createTask).mockResolvedValue(aTask());
    const { user } = renderWithApp(<TaskFormModal projectId={7} onClose={onClose} />);
    await screen.findByRole('option', { name: 'Ben Lee' });

    await user.type(screen.getByLabelText('Title'), '  Report  ');
    await user.type(screen.getByLabelText('Description'), 'Quarterly numbers');
    await user.selectOptions(screen.getByLabelText('Priority'), 'HIGH');
    setDueDate('2099-12-31');
    await user.selectOptions(screen.getByLabelText('Assignee'), 'Ben Lee');
    await user.click(screen.getByRole('button', { name: 'Create task' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(tasksApi.createTask).toHaveBeenCalledWith(7, {
      title: 'Report',
      description: 'Quarterly numbers',
      priority: 'HIGH',
      dueDate: '2099-12-31',
      assigneeId: 2,
    });
  });

  it('shows a server error next to its field and keeps the form open', async () => {
    jest.mocked(tasksApi.createTask).mockRejectedValue(
      new ApiError(400, 'VALIDATION_FAILED', 'Request validation failed', {
        dueDate: 'Due date cannot be in the past',
      }),
    );
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    await user.type(screen.getByLabelText('Title'), 'Report');
    await user.click(screen.getByRole('button', { name: 'Create task' }));

    expect(await screen.findByText('Due date cannot be in the past')).toBeInTheDocument();
    expect(screen.getByLabelText('Due date')).toBeInvalid();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('shows any other failure as a message for the whole form', async () => {
    jest.mocked(tasksApi.createTask).mockRejectedValue(new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server.'));
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    await user.type(screen.getByLabelText('Title'), 'Report');
    await user.click(screen.getByRole('button', { name: 'Create task' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the server.');
    expect(screen.getByLabelText('Title')).toHaveValue('Report');
  });

  it('shows that people are loading, and can retry if that fails', async () => {
    jest
      .mocked(usersApi.listUsers)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'Server problem'))
      .mockResolvedValue([asha, ben]);
    const { user } = renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    expect(screen.getByLabelText('Assignee')).toBeDisabled();
    expect(screen.getByRole('option', { name: 'Loading people…' })).toBeInTheDocument();

    await screen.findByText(/Could not load people/);
    expect(screen.getByLabelText('Assignee')).toBeEnabled();

    await user.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('option', { name: 'Ben Lee' })).toBeInTheDocument();
  });

  it('has no history section', () => {
    renderWithApp(<TaskFormModal projectId={1} onClose={onClose} />);

    expect(screen.queryByRole('heading', { name: 'History' })).not.toBeInTheDocument();
  });
});

describe('editing a task', () => {
  const overdueTask = aTask({
    id: 55,
    title: 'Old report',
    description: 'Details',
    priority: 'LOW',
    dueDate: '2020-01-01',
    overdue: true,
    assignee: ben,
  });

  it('starts with the current values', async () => {
    renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);
    await screen.findByRole('option', { name: 'Ben Lee' });

    expect(screen.getByRole('dialog', { name: 'Edit task' })).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toHaveValue('Old report');
    expect(screen.getByLabelText('Description')).toHaveValue('Details');
    expect(screen.getByLabelText('Priority')).toHaveValue('LOW');
    expect(screen.getByLabelText('Due date')).toHaveValue('2020-01-01');
    expect(screen.getByLabelText('Assignee')).toHaveValue('2');
  });

  it('lets an overdue task be saved without changing its date', async () => {
    jest.mocked(tasksApi.updateTask).mockResolvedValue(overdueTask);
    const { user } = renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);
    await screen.findByRole('option', { name: 'Ben Lee' });

    await user.type(screen.getByLabelText('Title'), ' updated');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(tasksApi.updateTask).toHaveBeenCalledWith(
      55,
      expect.objectContaining({ title: 'Old report updated', dueDate: '2020-01-01', assigneeId: 2 }),
    );
    expect(tasksApi.createTask).not.toHaveBeenCalled();
  });

  it('can clear the due date and the assignee', async () => {
    jest.mocked(tasksApi.updateTask).mockResolvedValue(overdueTask);
    const { user } = renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);
    await screen.findByRole('option', { name: 'Ben Lee' });

    setDueDate('');
    await user.selectOptions(screen.getByLabelText('Assignee'), 'Unassigned');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(tasksApi.updateTask).toHaveBeenCalledWith(55, expect.objectContaining({ dueDate: null, assigneeId: null }));
  });

  it('keeps the current assignee when the list of people fails to load', async () => {
    jest.mocked(usersApi.listUsers).mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'Server problem'));
    renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);

    await screen.findByText(/Could not load people/);

    expect(screen.getByLabelText('Assignee')).toHaveValue('2');
    expect(screen.getByRole('option', { name: 'Ben Lee' })).toBeInTheDocument();
  });

  it('shows who changed the status and when', async () => {
    jest.mocked(tasksApi.listTaskActivity).mockResolvedValue([
      { id: 1, actor: asha, fromStatus: 'TODO', toStatus: 'IN_PROGRESS', occurredAt: '2026-03-15T10:00:00Z' },
    ]);
    renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);

    expect(await screen.findByText('Asha Rao')).toBeInTheDocument();
    expect(screen.getByText(/moved this from To Do to In Progress/)).toBeInTheDocument();
  });

  it('has loading, empty and error states for the history', async () => {
    jest
      .mocked(tasksApi.listTaskActivity)
      .mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'Server problem'))
      .mockResolvedValue([]);
    const { user } = renderWithApp(<TaskFormModal projectId={1} task={overdueTask} onClose={onClose} />);

    expect(screen.getByText('Loading history…')).toBeInTheDocument();

    expect(await screen.findByText('Could not load the history.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('The status has not changed yet.')).toBeInTheDocument();
  });
});
