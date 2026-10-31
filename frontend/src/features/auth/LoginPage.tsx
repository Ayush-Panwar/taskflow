import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Brand } from '../../components/AppLayout';
import { Button } from '../../components/Button';
import { TextField } from '../../components/FormFields';
import { FormError } from '../../components/StateMessage';
import { useForm } from '../../hooks/useForm';
import { validateLogin, type LoginValues } from './validation';

const INITIAL_VALUES: LoginValues = { email: '', password: '' };

export function LoginPage() {
  const { login, sessionExpired } = useAuth();
  const location = useLocation();

  // After a successful login, PublicOnlyRoute sends the user to where they were going.
  const form = useForm({
    initialValues: INITIAL_VALUES,
    validate: validateLogin,
    onSubmit: (values) => login(values.email.trim(), values.password),
  });

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Brand />
        <h1 className="auth-card__title">Sign in</h1>
        {sessionExpired && (
          <p className="notice" role="status">
            Your session has expired. Please sign in again.
          </p>
        )}
        <form onSubmit={form.handleSubmit} noValidate>
          <TextField label="Email" type="email" autoComplete="email" {...form.field('email')} />
          <TextField label="Password" type="password" autoComplete="current-password" {...form.field('password')} />
          <FormError message={form.formError} />
          <Button type="submit" variant="primary" className="auth-card__submit" loading={form.submitting}>
            {form.submitting ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
        <p className="auth-card__switch">
          New to TaskFlow?{' '}
          <Link to="/register" state={location.state}>
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
