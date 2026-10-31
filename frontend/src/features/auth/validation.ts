import type { FieldErrors } from '../../hooks/useForm';

export interface LoginValues extends Record<string, string> {
  email: string;
  password: string;
}

export interface RegisterValues extends Record<string, string> {
  name: string;
  email: string;
  password: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail(email: string): string | undefined {
  if (!email.trim()) {
    return 'Email is required';
  }
  return EMAIL_PATTERN.test(email.trim()) ? undefined : 'Enter a valid email address';
}

export function validateLogin(values: LoginValues): FieldErrors<LoginValues> {
  return {
    email: validateEmail(values.email),
    password: values.password ? undefined : 'Password is required',
  };
}

export function validateRegister(values: RegisterValues): FieldErrors<RegisterValues> {
  const errors: FieldErrors<RegisterValues> = {
    name: values.name.trim() ? undefined : 'Name is required',
    email: validateEmail(values.email),
  };
  if (!values.password) {
    errors.password = 'Password is required';
  } else if (values.password.length < 8 || values.password.length > 72) {
    errors.password = 'Password must be between 8 and 72 characters';
  }
  return errors;
}
