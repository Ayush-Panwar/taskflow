import { todayIso } from '../../lib/dates';
import { aTask, ben } from '../../test/helpers';
import { EMPTY_TASK_FORM, formValuesToInput, taskToFormValues, validateTaskForm } from './taskForm';

describe('validateTaskForm', () => {
  const withTitle = { ...EMPTY_TASK_FORM, title: 'A task' };

  it('accepts a minimal task', () => {
    expect(validateTaskForm(withTitle, false)).toEqual({});
  });

  it('requires a title that is not just spaces', () => {
    expect(validateTaskForm({ ...EMPTY_TASK_FORM, title: '   ' }, false).title).toBe('Title is required');
  });

  it('allows 120 characters in the title and no more', () => {
    expect(validateTaskForm({ ...EMPTY_TASK_FORM, title: 'x'.repeat(120) }, false).title).toBeUndefined();
    expect(validateTaskForm({ ...EMPTY_TASK_FORM, title: 'x'.repeat(121) }, false).title).toBe(
      'Title must be at most 120 characters',
    );
  });

  it('rejects a past due date on a new task, but allows today', () => {
    expect(validateTaskForm({ ...withTitle, dueDate: '2020-01-01' }, false).dueDate).toBe(
      'Due date cannot be in the past',
    );
    expect(validateTaskForm({ ...withTitle, dueDate: todayIso() }, false).dueDate).toBeUndefined();
  });

  it('allows a past due date when editing, so overdue tasks stay editable', () => {
    expect(validateTaskForm({ ...withTitle, dueDate: '2020-01-01' }, true).dueDate).toBeUndefined();
  });
});

describe('form conversion', () => {
  it('turns blank optional fields into null and tidies the text', () => {
    const input = formValuesToInput({ ...EMPTY_TASK_FORM, title: '  Report ', description: '   ', priority: 'HIGH' });

    expect(input).toEqual({
      title: 'Report',
      description: null,
      priority: 'HIGH',
      dueDate: null,
      assigneeId: null,
    });
  });

  it('turns the selected assignee into a number', () => {
    expect(formValuesToInput({ ...EMPTY_TASK_FORM, assigneeId: '42' }).assigneeId).toBe(42);
  });

  it('fills the form from an existing task', () => {
    const values = taskToFormValues(
      aTask({ title: 'Report', description: 'Long', priority: 'LOW', dueDate: '2026-03-15', assignee: ben }),
    );

    expect(values).toEqual({
      title: 'Report',
      description: 'Long',
      priority: 'LOW',
      dueDate: '2026-03-15',
      assigneeId: '2',
    });
  });

  it('uses empty strings for the empty fields of an existing task', () => {
    expect(taskToFormValues(aTask({ description: null, dueDate: null, assignee: null }))).toMatchObject({
      description: '',
      dueDate: '',
      assigneeId: '',
    });
  });
});
