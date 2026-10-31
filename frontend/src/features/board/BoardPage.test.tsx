import { screen, waitFor, within } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { ApiError } from '../../api/client';
import * as projectsApi from '../../api/projects';
import * as tasksApi from '../../api/tasks';
import type { Task, TaskListParams, TaskStatus } from '../../api/types';
import * as usersApi from '../../api/users';
import { aPage, asha, aSession, aTask, ben, renderWithApp } from '../../test/helpers';
import { BoardPage } from './BoardPage';

jest.mock('../../api/projects');
jest.mock('../../api/tasks');
jest.mock('../../api/users');

const project = { id: 1, name: 'Website', description: 'New marketing site', createdAt: '2026-01-01T10:00:00Z' };

/** A tiny stand-in for the server: it answers the list call the way the real API would. */
function givenTasks(tasks: Task[]) {
  jest.mocked(tasksApi.listTasks).mockImplementation(async (_projectId, params: TaskListParams) => {
    const matching = tasks.filter(
      (task) =>
        task.status === params.status &&
        (!params.priority || task.priority === params.priority) &&
        (!params.assignee || task.assignee?.id === params.assignee),
    );
    return aPage(matching);
  });
}

function renderBoard(route = '/projects/1') {
  return renderWithApp(
    <Routes>
      <Route path="/projects/:projectId" element={<BoardPage />} />
    </Routes>,
    { route, session: aSession() },
  );
}

/** Renders the board and waits for the project to load, so that the columns exist. */
async function renderLoadedBoard(route?: string) {
  const view = renderBoard(route);
  await screen.findByRole('region', { name: 'To Do' });
  return view;
}

function column(status: 'To Do' | 'In Progress' | 'Done') {
  return within(screen.getByRole('region', { name: status }));
}

beforeEach(() => {
  jest.mocked(projectsApi.getProject).mockResolvedValue(project);
  jest.mocked(projectsApi.getProjectSummary).mockResolvedValue({
    projectId: 1,
    counts: { TODO: 2, IN_PROGRESS: 1, DONE: 0 },
    total: 3,
    overdue: 1,
  });
  jest.mocked(usersApi.listUsers).mockResolvedValue([asha, ben]);
  jest.mocked(tasksApi.listTaskActivity).mockResolvedValue([]);
  givenTasks([]);
});

describe('BoardPage', () => {
  it('shows tasks in their three columns, with a summary bar', async () => {
    givenTasks([
      aTask({ id: 1, title: 'First', status: 'TODO' }),
      aTask({ id: 2, title: 'Second', status: 'IN_PROGRESS' }),
    ]);
    await renderLoadedBoard();

    expect(await column('To Do').findByText('First')).toBeInTheDocument();
    expect(await column('In Progress').findByText('Second')).toBeInTheDocument();
    expect(await column('Done').findByText('No tasks here')).toBeInTheDocument();

    const summary = await screen.findByLabelText('Project summary');
    expect(summary).toHaveTextContent('To Do2');
    expect(summary).toHaveTextContent('Overdue1');
  });

  it('asks the server for each column separately, using the status filter', async () => {
    await renderLoadedBoard();

    await column('Done').findByText('No tasks here');

    const requestedStatuses = jest.mocked(tasksApi.listTasks).mock.calls.map(([, params]) => params.status);
    expect(new Set(requestedStatuses)).toEqual(new Set<TaskStatus>(['TODO', 'IN_PROGRESS', 'DONE']));
  });

  it('shows a loading state while waiting', async () => {
    jest.mocked(projectsApi.getProject).mockReturnValue(new Promise(() => {}));
    renderBoard();

    expect(screen.getByText('Loading project…')).toBeInTheDocument();
  });

  it('shows loading placeholders for the summary and each column', async () => {
    jest.mocked(projectsApi.getProjectSummary).mockReturnValue(new Promise(() => {}));
    jest.mocked(tasksApi.listTasks).mockReturnValue(new Promise(() => {}));
    renderBoard();

    await screen.findByRole('heading', { name: 'Website' });

    expect(screen.getByText('Loading summary')).toBeInTheDocument();
    expect(screen.getAllByText('Loading tasks')).toHaveLength(3);
  });

  it('explains when a project does not exist or belongs to someone else, with no retry button', async () => {
    jest.mocked(projectsApi.getProject).mockRejectedValue(new ApiError(404, 'NOT_FOUND', 'Project not found'));
    renderBoard();

    expect(await screen.findByText('Project not found')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to projects' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument();
  });

  it('treats a nonsense project id as not found without calling the server', () => {
    renderBoard('/projects/not-a-number');

    expect(screen.getByText('Project not found')).toBeInTheDocument();
    expect(projectsApi.getProject).not.toHaveBeenCalled();
  });

  it('offers a retry when the project cannot be loaded', async () => {
    jest
      .mocked(projectsApi.getProject)
      .mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server.'))
      .mockResolvedValue(project);
    const { user } = renderBoard();

    await user.click(await screen.findByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('heading', { name: 'Website' })).toBeInTheDocument();
  });

  it('shows errors for the summary and the columns, and retrying recovers', async () => {
    jest.mocked(projectsApi.getProjectSummary).mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'No connection.'));
    const failure = new ApiError(0, 'NETWORK_ERROR', 'No connection.');
    jest
      .mocked(tasksApi.listTasks)
      .mockRejectedValueOnce(failure)
      .mockRejectedValueOnce(failure)
      .mockRejectedValueOnce(failure);
    const { user } = renderBoard();

    expect(await screen.findByText(/Could not load the summary/)).toBeInTheDocument();
    expect(await screen.findAllByText('Could not load tasks')).toHaveLength(3);

    for (const button of screen.getAllByRole('button', { name: 'Try again' })) {
      await user.click(button);
    }

    expect(await screen.findByLabelText('Project summary')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('Could not load tasks')).not.toBeInTheDocument());
  });

  it('sends the filters from the URL to the server', async () => {
    renderBoard('/projects/1?priority=HIGH&assignee=2&sort=priority,desc');

    await screen.findByRole('heading', { name: 'Website' });

    await waitFor(() =>
      expect(tasksApi.listTasks).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ status: 'TODO', priority: 'HIGH', assignee: 2, sort: 'priority,desc', page: 0 }),
        expect.anything(),
      ),
    );
  });

  it('ignores nonsense filter values in the URL rather than failing', async () => {
    renderBoard('/projects/1?priority=URGENT&assignee=abc&sort=password');

    await screen.findByRole('heading', { name: 'Website' });

    await waitFor(() => expect(tasksApi.listTasks).toHaveBeenCalled());
    expect(jest.mocked(tasksApi.listTasks).mock.calls[0][1]).toMatchObject({
      priority: undefined,
      assignee: undefined,
      sort: 'dueDate,asc',
    });
  });

  it('filters by priority and assignee, says when nothing matches, and can clear', async () => {
    givenTasks([
      aTask({ id: 1, title: 'High for Ben', priority: 'HIGH', assignee: ben }),
      aTask({ id: 2, title: 'Low for Asha', priority: 'LOW', assignee: asha }),
    ]);
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Low for Asha');
    await screen.findByRole('option', { name: 'Ben Lee' });

    await user.selectOptions(screen.getByLabelText('Priority'), 'HIGH');
    await waitFor(() => expect(column('To Do').queryByText('Low for Asha')).not.toBeInTheDocument());
    expect(column('To Do').getByText('High for Ben')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Assignee'), 'Asha Rao');
    expect(await column('To Do').findByText('No tasks match these filters')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(await column('To Do').findByText('Low for Asha')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Clear filters' })).not.toBeInTheDocument();
  });

  it('asks the server for the chosen sort order', async () => {
    const { user } = await renderLoadedBoard();
    await column('Done').findByText('No tasks here');

    await user.selectOptions(screen.getByLabelText('Sort by'), 'priority,desc');

    await waitFor(() =>
      expect(tasksApi.listTasks).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ sort: 'priority,desc' }),
        expect.anything(),
      ),
    );
  });

  it('loads further pages of a column on request', async () => {
    jest.mocked(tasksApi.listTasks).mockImplementation(async (_projectId, params) => {
      if (params.status !== 'TODO') {
        return aPage([]);
      }
      return params.page === 0
        ? aPage([aTask({ id: 1, title: 'On page one' })], { totalElements: 2, totalPages: 2, hasNext: true })
        : aPage([aTask({ id: 2, title: 'On page two' })], { page: 1, totalElements: 2, totalPages: 2 });
    });
    const { user } = await renderLoadedBoard();

    await column('To Do').findByText('On page one');
    expect(column('To Do').queryByText('On page two')).not.toBeInTheDocument();

    await user.click(column('To Do').getByRole('button', { name: 'Load more (1 left)' }));

    expect(await column('To Do').findByText('On page two')).toBeInTheDocument();
    expect(column('To Do').getByText('On page one')).toBeInTheDocument();
    expect(column('To Do').queryByRole('button', { name: /Load more/ })).not.toBeInTheDocument();
  });

  it('moves a task immediately, and the move is saved through the API', async () => {
    const tasks = [aTask({ id: 1, title: 'Report', status: 'TODO' })];
    givenTasks(tasks);
    let finishSaving: (task: Task) => void = () => {};
    jest.mocked(tasksApi.updateTaskStatus).mockReturnValue(new Promise((resolve) => (finishSaving = resolve)));
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');

    expect(await column('Done').findByText('Report')).toBeInTheDocument();
    expect(column('To Do').queryByText('Report')).not.toBeInTheDocument();
    expect(screen.getByText('Saving…')).toBeInTheDocument();
    expect(tasksApi.updateTaskStatus).toHaveBeenCalledWith(1, 'DONE');

    const saved = { ...tasks[0], status: 'DONE' as const };
    givenTasks([saved]);
    finishSaving(saved);

    await waitFor(() => expect(screen.queryByText('Saving…')).not.toBeInTheDocument());
    expect(column('Done').getByText('Report')).toBeInTheDocument();
  });

  it('saves repeated moves of a card one at a time, in order', async () => {
    givenTasks([aTask({ id: 1, title: 'Report', status: 'TODO' })]);
    const finishSaving: ((task: Task) => void)[] = [];
    jest
      .mocked(tasksApi.updateTaskStatus)
      .mockImplementation(() => new Promise((resolve) => finishSaving.push(resolve)));
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'IN_PROGRESS');
    await column('In Progress').findByText('Report');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');
    await column('Done').findByText('Report');

    expect(tasksApi.updateTaskStatus).toHaveBeenCalledTimes(1);
    expect(tasksApi.updateTaskStatus).toHaveBeenLastCalledWith(1, 'IN_PROGRESS');

    finishSaving[0](aTask({ id: 1, title: 'Report', status: 'IN_PROGRESS' }));

    await waitFor(() => expect(tasksApi.updateTaskStatus).toHaveBeenCalledTimes(2));
    expect(tasksApi.updateTaskStatus).toHaveBeenLastCalledWith(1, 'DONE');
  });

  it('removes the overdue highlight straight away when a task is finished', async () => {
    givenTasks([aTask({ id: 1, title: 'Report', overdue: true, dueDate: '2020-01-01' })]);
    jest.mocked(tasksApi.updateTaskStatus).mockReturnValue(new Promise(() => {}));
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Overdue');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');

    await column('Done').findByText('Report');
    expect(column('Done').queryByText('Overdue')).not.toBeInTheDocument();
  });

  it('puts a task back and explains why if saving the move fails', async () => {
    givenTasks([aTask({ id: 1, title: 'Report', status: 'TODO' })]);
    jest
      .mocked(tasksApi.updateTaskStatus)
      .mockRejectedValue(new ApiError(409, 'CONCURRENT_UPDATE', 'Someone else changed this task.'));
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');

    expect(await screen.findByText('Could not move the task: Someone else changed this task.')).toBeInTheDocument();
    expect(await column('To Do').findByText('Report')).toBeInTheDocument();
    expect(column('Done').queryByText('Report')).not.toBeInTheDocument();
  });

  it('refreshes the summary after a move', async () => {
    givenTasks([aTask({ id: 1, title: 'Report' })]);
    jest.mocked(tasksApi.updateTaskStatus).mockResolvedValue(aTask({ id: 1, title: 'Report', status: 'DONE' }));
    const { user } = await renderLoadedBoard();
    await screen.findByLabelText('Project summary');
    await column('To Do').findByText('Report');
    const callsBefore = jest.mocked(projectsApi.getProjectSummary).mock.calls.length;

    await user.selectOptions(screen.getByRole('combobox', { name: 'Status of Report' }), 'DONE');

    await waitFor(() =>
      expect(jest.mocked(projectsApi.getProjectSummary).mock.calls.length).toBeGreaterThan(callsBefore),
    );
  });

  it('deletes a task after confirmation, and refreshes', async () => {
    const tasks = [aTask({ id: 1, title: 'Report' })];
    givenTasks(tasks);
    jest.mocked(tasksApi.deleteTask).mockImplementation(async () => {
      givenTasks([]);
    });
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.click(screen.getByRole('button', { name: 'Delete Report' }));
    const dialog = within(screen.getByRole('dialog', { name: 'Delete this task?' }));
    expect(tasksApi.deleteTask).not.toHaveBeenCalled();
    await user.click(dialog.getByRole('button', { name: 'Delete task' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(tasksApi.deleteTask).toHaveBeenCalledWith(1);
    expect(await column('To Do').findByText('No tasks here')).toBeInTheDocument();
  });

  it('does nothing if deleting is cancelled, and shows an error if it fails', async () => {
    givenTasks([aTask({ id: 1, title: 'Report' })]);
    jest.mocked(tasksApi.deleteTask).mockRejectedValue(new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server.'));
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.click(screen.getByRole('button', { name: 'Delete Report' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(tasksApi.deleteTask).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Delete Report' }));
    await user.click(screen.getByRole('button', { name: 'Delete task' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the server.');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(column('To Do').getByText('Report')).toBeInTheDocument();
  });

  it('opens the form to create and to edit a task', async () => {
    givenTasks([aTask({ id: 1, title: 'Report' })]);
    const { user } = await renderLoadedBoard();
    await column('To Do').findByText('Report');

    await user.click(screen.getByRole('button', { name: 'New task' }));
    expect(screen.getByRole('dialog', { name: 'New task' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    await user.click(screen.getByRole('button', { name: 'Edit Report' }));
    expect(screen.getByRole('dialog', { name: 'Edit task' })).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toHaveValue('Report');
  });
});
