import type { ButtonHTMLAttributes } from 'react';
import { Spinner } from './Spinner';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'regular' | 'small';
  /** Shows a spinner and blocks clicks while a request is running. */
  loading?: boolean;
}

export function Button({
  variant = 'secondary',
  size = 'regular',
  loading = false,
  disabled,
  className = '',
  type = 'button',
  children,
  ...rest
}: ButtonProps) {
  const classes = ['btn', `btn--${variant}`, size === 'small' ? 'btn--small' : '', className].filter(Boolean).join(' ');
  return (
    <button {...rest} type={type} className={classes} disabled={disabled || loading} aria-busy={loading}>
      {loading && <Spinner size="small" />}
      {children}
    </button>
  );
}
