import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute, PublicOnlyRoute } from './auth/routes';
import { AppLayout } from './components/AppLayout';
import { EmptyState } from './components/StateMessage';
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { BoardPage } from './features/board/BoardPage';
import { ProjectsPage } from './features/projects/ProjectsPage';

function NotFoundPage() {
  return (
    <main className="page">
      <EmptyState title="Page not found" action={<Link to="/projects">Go to your projects</Link>} />
    </main>
  );
}

export function App() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:projectId" element={<BoardPage />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/projects" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
