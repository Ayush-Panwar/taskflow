import type { InfiniteData } from '@tanstack/react-query';
import type { Page, Task, TaskStatus } from '../../api/types';

export type TaskPages = InfiniteData<Page<Task>, number>;

/**
 * Applies a move to one column's cached pages, so the card jumps straight away. It leaves the
 * column it came from and goes to the top of the column it moved to. The refetch that follows
 * the request puts it in its properly sorted position.
 */
export function moveTaskInPages(pages: TaskPages, columnStatus: TaskStatus, moved: Task, fromStatus: TaskStatus): TaskPages {
  if (columnStatus === fromStatus) {
    return {
      ...pages,
      pages: pages.pages.map((page) => ({
        ...page,
        content: page.content.filter((task) => task.id !== moved.id),
        totalElements: page.totalElements - 1,
      })),
    };
  }
  if (columnStatus === moved.status) {
    return {
      ...pages,
      pages: pages.pages.map((page, index) => ({
        ...page,
        content: index === 0 ? [moved, ...page.content] : page.content,
        totalElements: page.totalElements + 1,
      })),
    };
  }
  return pages;
}
