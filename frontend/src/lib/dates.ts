/*
 * Due dates are calendar dates (yyyy-MM-dd) with no time or time zone. new Date('2026-03-15') would
 * read one as midnight UTC, which displays as March 14 anywhere west of Greenwich, so these helpers
 * always work in local time.
 */

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** Today in the user's own time zone, as yyyy-MM-dd. */
export function todayIso(now: Date = new Date()): string {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function parseCalendarDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function formatDueDate(value: string): string {
  const date = parseCalendarDate(value);
  const sameYear = date.getFullYear() === new Date().getFullYear();
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: sameYear ? undefined : 'numeric',
  });
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
