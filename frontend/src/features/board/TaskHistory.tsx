import { Button } from '../../components/Button';
import { Spinner } from '../../components/Spinner';
import { formatDateTime } from '../../lib/dates';
import { STATUS_LABELS } from '../../lib/taskLabels';
import { useTaskActivity } from './taskQueries';

export function TaskHistory({ taskId }: { taskId: number }) {
  const activity = useTaskActivity(taskId);

  return (
    <section className="history" aria-label="Status history">
      <h3 className="history__title">History</h3>

      {activity.isPending && (
        <p className="history__empty" role="status">
          <Spinner size="small" /> Loading history…
        </p>
      )}

      {activity.isError && !activity.data && (
        <div className="history__error" role="alert">
          <span>Could not load the history.</span>
          <Button size="small" onClick={() => activity.refetch()} loading={activity.isFetching}>
            Try again
          </Button>
        </div>
      )}

      {activity.data?.length === 0 && <p className="history__empty">The status has not changed yet.</p>}

      {activity.data && activity.data.length > 0 && (
        <ol className="history__list">
          {activity.data.map((entry) => (
            <li key={entry.id} className="history__item">
              <span>
                <strong>{entry.actor.name}</strong> moved this from {STATUS_LABELS[entry.fromStatus]} to{' '}
                {STATUS_LABELS[entry.toStatus]}
              </span>
              <time className="history__time" dateTime={entry.occurredAt}>
                {formatDateTime(entry.occurredAt)}
              </time>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
