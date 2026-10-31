import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { Project } from '../../api/types';
import { Button } from '../../components/Button';
import { LoadingMessage } from '../../components/Spinner';
import { EmptyState, ErrorState } from '../../components/StateMessage';
import { ProjectFormModal } from './ProjectFormModal';
import { useProjects } from './projectQueries';

function ProjectCard({ project }: { project: Project }) {
  return (
    <Link to={`/projects/${project.id}`} className="project-card">
      <h2 className="project-card__name">{project.name}</h2>
      <p className="project-card__description">{project.description ?? 'No description'}</p>
    </Link>
  );
}

export function ProjectsPage() {
  const projects = useProjects();
  const [creating, setCreating] = useState(false);

  const openForm = () => setCreating(true);

  return (
    <>
      <div className="page-header">
        <h1 className="page-header__title">Projects</h1>
        <Button variant="primary" onClick={openForm}>
          New project
        </Button>
      </div>

      {projects.isPending && <LoadingMessage label="Loading your projects…" />}

      {projects.isError && !projects.data && (
        <ErrorState
          title="Could not load your projects"
          message={projects.error.message}
          onRetry={() => projects.refetch()}
          retrying={projects.isFetching}
        />
      )}

      {projects.data?.length === 0 && (
        <EmptyState
          title="No projects yet"
          description="Create your first project to start tracking tasks."
          action={
            <Button variant="primary" onClick={openForm}>
              Create a project
            </Button>
          }
        />
      )}

      {projects.data && projects.data.length > 0 && (
        <div className="project-grid">
          {projects.data.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}

      {creating && <ProjectFormModal onClose={() => setCreating(false)} />}
    </>
  );
}
