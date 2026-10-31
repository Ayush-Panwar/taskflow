import { formatDueDate, todayIso } from './dates';

// The test run uses a time zone west of UTC. See jest.config.cjs.
describe('dates', () => {
  it('runs in a zone where UTC handling would show the wrong day', () => {
    expect(new Date('2026-03-15').getDate()).toBe(14);
  });

  it('shows a due date on the day it was set for', () => {
    expect(formatDueDate('2026-03-15')).toContain('15');
  });

  it('works out today from local time, not UTC', () => {
    const lateEvening = new Date(2026, 2, 15, 23, 30); // Already March 16 in UTC.

    expect(todayIso(lateEvening)).toBe('2026-03-15');
  });

  it('pads months and days to two digits', () => {
    expect(todayIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
