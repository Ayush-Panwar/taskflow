import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { aTask, ben } from '../../test/helpers';
import { TaskCard, type TaskCardHandlers } from './TaskCard';

function handlers(): jest.Mocked<TaskCardHandlers> {
  return { onEdit: jest.fn(), onDelete: jest.fn(), onStatusChange: jest.fn() };
}

describe('TaskCard', () => {
  it('shows the details of a task', () => {
    const task = aTask({
      title: 'Fix the login page',
      description: 'It crashes on Safari',
      priority: 'HIGH',
      dueDate: '2026-03-15',
      assignee: ben,
    });

    render(<TaskCard task={task} handlers={handlers()} />);

    expect(screen.getByRole('heading', { name: 'Fix the login page' })).toBeInTheDocument();
    expect(screen.getByText('It crashes on Safari')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByText(/^Due .*15/)).toBeInTheDocument();
    expect(screen.getByText('Ben Lee')).toBeInTheDocument();
    expect(screen.getByText('BL')).toBeInTheDocument();
  });

  it('says so when a task has no date and no assignee', () => {
    render(<TaskCard task={aTask({ dueDate: null, assignee: null })} handlers={handlers()} />);

    expect(screen.getByText('No due date')).toBeInTheDocument();
    expect(screen.getByText('Unassigned')).toBeInTheDocument();
  });

  it('highlights an overdue task with a visible label, not just a color', () => {
    render(<TaskCard task={aTask({ overdue: true, dueDate: '2020-01-01' })} handlers={handlers()} />);

    expect(screen.getByRole('article')).toHaveClass('task-card--overdue');
    expect(screen.getByText('Overdue')).toBeInTheDocument();
  });

  it('does not highlight a task that is not overdue', () => {
    render(<TaskCard task={aTask({ overdue: false, dueDate: '2020-01-01', status: 'DONE' })} handlers={handlers()} />);

    expect(screen.getByRole('article')).not.toHaveClass('task-card--overdue');
    expect(screen.queryByText('Overdue')).not.toBeInTheDocument();
  });

  it('moves a task using the status dropdown', async () => {
    const task = aTask({ title: 'Report', status: 'TODO' });
    const cardHandlers = handlers();
    render(<TaskCard task={task} handlers={cardHandlers} />);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');

    expect(cardHandlers.onStatusChange).toHaveBeenCalledWith(task, 'DONE');
  });

  it('opens the edit form from the button or the title, and asks to delete from its button', async () => {
    const task = aTask({ title: 'Report' });
    const cardHandlers = handlers();
    render(<TaskCard task={task} handlers={cardHandlers} />);

    await userEvent.click(screen.getByRole('button', { name: 'Edit Report' }));
    await userEvent.click(screen.getByRole('button', { name: 'Report' }));
    await userEvent.click(screen.getByRole('button', { name: 'Delete Report' }));

    expect(cardHandlers.onEdit).toHaveBeenCalledTimes(2);
    expect(cardHandlers.onDelete).toHaveBeenCalledWith(task);
  });

  it('shows when a move is still being saved', () => {
    render(<TaskCard task={aTask()} handlers={handlers()} saving />);

    expect(screen.getByText('Saving…')).toBeInTheDocument();
    expect(screen.getByRole('article')).toHaveAttribute('aria-busy', 'true');
  });

  it('shows no controls on the copy that follows the cursor during a drag', () => {
    render(<TaskCard task={aTask({ title: 'Report' })} />);

    expect(screen.getByText('Report')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });
});
