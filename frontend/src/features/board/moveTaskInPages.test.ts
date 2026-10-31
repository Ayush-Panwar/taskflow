import { aPage, aTask } from '../../test/helpers';
import { moveTaskInPages, type TaskPages } from './moveTaskInPages';

function pagesOf(...pages: ReturnType<typeof aPage<ReturnType<typeof aTask>>>[]): TaskPages {
  return { pages, pageParams: pages.map((_, index) => index) };
}

describe('moveTaskInPages', () => {
  const task = aTask({ id: 1, status: 'TODO' });
  const other = aTask({ id: 2, status: 'TODO' });
  const moved = { ...task, status: 'DONE' as const };

  it('removes the task from the column it left and lowers the total', () => {
    const column = pagesOf(aPage([task, other], { totalElements: 2 }));

    const result = moveTaskInPages(column, 'TODO', moved, 'TODO');

    expect(result.pages[0].content).toEqual([other]);
    expect(result.pages[0].totalElements).toBe(1);
  });

  it('finds the task even when it is on a later page', () => {
    const column = pagesOf(aPage([other], { totalElements: 2 }), aPage([task], { page: 1, totalElements: 2 }));

    const result = moveTaskInPages(column, 'TODO', moved, 'TODO');

    expect(result.pages[1].content).toEqual([]);
    expect(result.pages.every((page) => page.totalElements === 1)).toBe(true);
  });

  it('adds the task to the top of the column it joined and raises the total', () => {
    const done = aTask({ id: 3, status: 'DONE' });
    const column = pagesOf(aPage([done], { totalElements: 1 }));

    const result = moveTaskInPages(column, 'DONE', moved, 'TODO');

    expect(result.pages[0].content).toEqual([moved, done]);
    expect(result.pages[0].totalElements).toBe(2);
  });

  it('leaves the third column unchanged', () => {
    const column = pagesOf(aPage([aTask({ id: 4, status: 'IN_PROGRESS' })]));

    expect(moveTaskInPages(column, 'IN_PROGRESS', moved, 'TODO')).toBe(column);
  });

  it('does not change the cached data it was given', () => {
    const column = pagesOf(aPage([task, other]));

    moveTaskInPages(column, 'TODO', moved, 'TODO');

    expect(column.pages[0].content).toEqual([task, other]);
  });
});
