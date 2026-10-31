import type { ReactNode } from 'react';
import { Button } from './Button';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="state state--empty">
      <p className="state__title">{title}</p>
      {description && <p className="state__text">{description}</p>}
      {action}
    </div>
  );
}

interface ErrorStateProps {
  title?: string;
  message: string;
  /** Leave out when trying again could not help, such as a project that does not exist. */
  onRetry?: () => void;
  retrying?: boolean;
  action?: ReactNode;
}

export function ErrorState({ title = 'Something went wrong', message, onRetry, retrying, action }: ErrorStateProps) {
  return (
    <div className="state state--error" role="alert">
      <p className="state__title">{title}</p>
      <p className="state__text">{message}</p>
      {onRetry && (
        <Button size="small" onClick={onRetry} loading={retrying}>
          Try again
        </Button>
      )}
      {action}
    </div>
  );
}

/** An error message for the whole form, shown above its buttons. */
export function FormError({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <p className="form-error" role="alert">
      {message}
    </p>
  );
}
