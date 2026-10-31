import { Button } from '../../components/Button';
import { SelectField } from '../../components/FormFields';
import { PRIORITIES, PRIORITY_LABELS } from '../../lib/taskLabels';
import { useUsers } from './taskQueries';
import { SORT_OPTIONS, useBoardFilters } from './useBoardFilters';

type BoardFiltersBarProps = ReturnType<typeof useBoardFilters>;

export function BoardFiltersBar({
  filters,
  isFiltering,
  setPriority,
  setAssignee,
  setSort,
  clearFilters,
}: BoardFiltersBarProps) {
  const users = useUsers();

  return (
    <div className="filters" role="group" aria-label="Filter and sort tasks">
      <SelectField label="Priority" value={filters.priority ?? ''} onChange={(event) => setPriority(event.target.value)}>
        <option value="">All priorities</option>
        {PRIORITIES.map((priority) => (
          <option key={priority} value={priority}>
            {PRIORITY_LABELS[priority]}
          </option>
        ))}
      </SelectField>

      <SelectField
        label="Assignee"
        value={filters.assignee ?? ''}
        disabled={users.isPending}
        onChange={(event) => setAssignee(event.target.value)}
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
      >
        <option value="">{users.isPending ? 'Loading people…' : 'Everyone'}</option>
        {users.data?.map((user) => (
          <option key={user.id} value={user.id}>
            {user.name}
          </option>
        ))}
      </SelectField>

      <SelectField label="Sort by" value={filters.sort} onChange={(event) => setSort(event.target.value)}>
        {SORT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>

      {isFiltering && (
        <Button size="small" variant="ghost" className="filters__clear" onClick={clearFilters}>
          Clear filters
        </Button>
      )}
    </div>
  );
}
