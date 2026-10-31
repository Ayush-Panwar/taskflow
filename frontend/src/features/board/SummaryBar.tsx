import { Button } from '../../components/Button';
import { STATUS_LABELS, STATUSES } from '../../lib/taskLabels';
import { useProjectSummary } from './taskQueries';

export function SummaryBar({ projectId }: { projectId: number }) {
  const summary = useProjectSummary(projectId);

  if (summary.isPending) {
    return (
      <div className="summary" role="status">
        <span className="visually-hidden">Loading summary</span>
        {[1, 2, 3, 4].map((tile) => (
          <div key={tile} className="skeleton skeleton--tile" />
        ))}
      </div>
    );
  }

  if (summary.isError && !summary.data) {
    return (
      <div className="summary-error" role="alert">
        <span>Could not load the summary: {summary.error.message}</span>
        <Button size="small" onClick={() => summary.refetch()} loading={summary.isFetching}>
          Try again
        </Button>
      </div>
    );
  }

  const { counts, overdue } = summary.data;

  return (
    <dl className="summary" aria-label="Project summary">
      {STATUSES.map((status) => (
        <div className="summary__tile" key={status}>
          <dt className="summary__label">{STATUS_LABELS[status]}</dt>
          <dd className="summary__value">{counts[status]}</dd>
        </div>
      ))}
      <div className={`summary__tile ${overdue > 0 ? 'summary__tile--alert' : ''}`}>
        <dt className="summary__label">Overdue</dt>
        <dd className="summary__value">{overdue}</dd>
      </div>
    </dl>
  );
}
