import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Brand } from '../../components/AppLayout';
import { Button } from '../../components/Button';
import { TextField } from '../../components/FormFields';
import { FormError } from '../../components/StateMessage';
import { useForm } from '../../hooks/useForm';
import { validateRegister, type RegisterValues } from './validation';

const INITIAL_VALUES: RegisterValues = { name: '', email: '', password: '' };

export function RegisterPage() {
  const { register } = useAuth();
  const location = useLocation();

  const form = useForm({
    initialValues: INITIAL_VALUES,
    validate: validateRegister,
    onSubmit: (values) => register(values.name.trim(), values.email.trim(), values.password),
  });

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Brand />
        <h1 className="auth-card__title">Create your account</h1>
        <form onSubmit={form.handleSubmit} noValidate>
          <TextField label="Name" autoComplete="name" {...form.field('name')} />
          <TextField label="Email" type="email" autoComplete="email" {...form.field('email')} />
          <TextField
            label="Password"
            type="password"
            autoComplete="new-password"
            hint="At least 8 characters"
            {...form.field('password')}
          />
          <FormError message={form.formError} />
          <Button type="submit" variant="primary" className="auth-card__submit" loading={form.submitting}>
            {form.submitting ? 'Creating account…' : 'Create account'}
          </Button>
        </form>
        <p className="auth-card__switch">
          Already have an account?{' '}
          <Link to="/login" state={location.state}>
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
