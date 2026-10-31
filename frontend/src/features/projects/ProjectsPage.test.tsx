import { fireEvent, screen, waitFor } from '@testing-library/react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { ApiError } from '../../api/client';
import * as projectsApi from '../../api/projects';
import { aSession, renderWithApp } from '../../test/helpers';
import { ProjectsPage } from './ProjectsPage';

jest.mock('../../api/projects');

const website = {
  id: 7,
  name: 'Website',
  description: 'New marketing site',
  createdAt: '2026-01-01T10:00:00Z',
};

function ShowLocation() {
  return <p>Now at {useLocation().pathname}</p>;
}

function renderPage() {
  return renderWithApp(
    <Routes>
      <Route path="/projects" element={<ProjectsPage />} />
      <Route path="/projects/:id" element={<ShowLocation />} />
    </Routes>,
    { route: '/projects', session: aSession() },
  );
}

describe('ProjectsPage', () => {
  it('shows a loading state, then the projects', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([website]);
    renderPage();

    expect(screen.getByText('Loading your projects…')).toBeInTheDocument();

    const link = await screen.findByRole('link', { name: /Website/ });
    expect(link).toHaveAttribute('href', '/projects/7');
    expect(screen.getByText('New marketing site')).toBeInTheDocument();
  });

  it('shows an empty state that offers to create a project', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([]);
    const { user } = renderPage();

    expect(await screen.findByText('No projects yet')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Create a project' }));
    expect(screen.getByRole('dialog', { name: 'New project' })).toBeInTheDocument();
  });

  it('shows an error state, and retrying can recover', async () => {
    jest
      .mocked(projectsApi.listProjects)
      .mockRejectedValueOnce(new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server.'))
      .mockResolvedValue([website]);
    const { user } = renderPage();

    expect(await screen.findByText('Could not reach the server.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('link', { name: /Website/ })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('creates a project and opens its board', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([]);
    jest.mocked(projectsApi.createProject).mockResolvedValue({ ...website, id: 42 });
    const { user } = renderPage();
    await user.click(await screen.findByRole('button', { name: 'New project' }));

    await user.type(screen.getByLabelText('Name'), '  Website  ');
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    expect(await screen.findByText('Now at /projects/42')).toBeInTheDocument();
    expect(jest.mocked(projectsApi.createProject).mock.calls[0][0]).toEqual({ name: 'Website', description: null });
  });

  it('requires a name, and keeps the form open if the server rejects it', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([]);
    jest
      .mocked(projectsApi.createProject)
      .mockRejectedValue(new ApiError(400, 'VALIDATION_FAILED', 'Failed', { name: 'That name is not allowed' }));
    const { user } = renderPage();
    await user.click(await screen.findByRole('button', { name: 'New project' }));

    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(screen.getByText('Name is required')).toBeInTheDocument();
    expect(projectsApi.createProject).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('Name'), 'Website');
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    expect(await screen.findByText('That name is not allowed')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('closes the form on Cancel and on Escape without saving', async () => {
    jest.mocked(projectsApi.listProjects).mockResolvedValue([]);
    const { user } = renderPage();
    await user.click(await screen.findByRole('button', { name: 'New project' }));

    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: 'New project' }));
    fireEvent(screen.getByRole('dialog'), new Event('cancel')); // What the browser sends on Escape.
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    expect(projectsApi.createProject).not.toHaveBeenCalled();
  });
});
