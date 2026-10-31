import { useSearchParams } from 'react-router-dom';
import type { TaskPriority } from '../../api/types';
import { PRIORITIES } from '../../lib/taskLabels';

export const SORT_OPTIONS = [
  { value: 'dueDate,asc', label: 'Due date: soonest first' },
  { value: 'dueDate,desc', label: 'Due date: latest first' },
  { value: 'priority,desc', label: 'Priority: highest first' },
  { value: 'priority,asc', label: 'Priority: lowest first' },
];

const DEFAULT_SORT = SORT_OPTIONS[0].value;

export interface BoardFilters {
  priority?: TaskPriority;
  assignee?: number;
  sort: string;
}

function parsePriority(value: string | null): TaskPriority | undefined {
  return PRIORITIES.find((priority) => priority === value);
}

function parseAssignee(value: string | null): number | undefined {
  const assignee = Number(value);
  return Number.isInteger(assignee) && assignee > 0 ? assignee : undefined;
}

function parseSort(value: string | null): string {
  return SORT_OPTIONS.some((option) => option.value === value) ? (value as string) : DEFAULT_SORT;
}

/**
 * Filters live in the URL, so a filtered board can be reloaded, bookmarked and shared, and the back
 * button works. Hand-edited values that make no sense are ignored rather than causing an error.
 */
export function useBoardFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters: BoardFilters = {
    priority: parsePriority(searchParams.get('priority')),
    assignee: parseAssignee(searchParams.get('assignee')),
    sort: parseSort(searchParams.get('sort')),
  };

  function setParam(name: 'priority' | 'assignee' | 'sort', value: string) {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        if (value) {
          next.set(name, value);
        } else {
          next.delete(name);
        }
        return next;
      },
      { replace: true },
    );
  }

  return {
    filters,
    isFiltering: filters.priority !== undefined || filters.assignee !== undefined,
    setPriority: (value: string) => setParam('priority', value),
    setAssignee: (value: string) => setParam('assignee', value),
    setSort: (value: string) => setParam('sort', value),
    clearFilters: () => setSearchParams({}, { replace: true }),
  };
}
