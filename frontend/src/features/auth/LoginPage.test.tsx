import { screen } from '@testing-library/react';
import * as authApi from '../../api/auth';
import { ApiError } from '../../api/client';
import { renderWithApp } from '../../test/helpers';
import { LoginPage } from './LoginPage';

jest.mock('../../api/auth');

describe('LoginPage', () => {
  it('asks for both fields before calling the server', async () => {
    const { user } = renderWithApp(<LoginPage />);

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('rejects a badly formed email', async () => {
    const { user } = renderWithApp(<LoginPage />);

    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('links each error message to its field for screen readers', async () => {
    const { user } = renderWithApp(<LoginPage />);

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    const email = screen.getByLabelText('Email');
    expect(email).toBeInvalid();
    expect(email).toHaveAccessibleDescription('Email is required');
  });

  it('clears a field error once the user starts fixing it', async () => {
    const { user } = renderWithApp(<LoginPage />);
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await user.type(screen.getByLabelText('Email'), 'a');

    expect(screen.queryByText('Email is required')).not.toBeInTheDocument();
  });

  it('shows the message from the server when the login fails', async () => {
    jest
      .mocked(authApi.login)
      .mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'Invalid email or password'));
    const { user } = renderWithApp(<LoginPage />);

    await user.type(screen.getByLabelText('Email'), 'asha@example.com');
    await user.type(screen.getByLabelText('Password'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    expect(screen.queryByText(/session has expired/i)).not.toBeInTheDocument();
  });

  it('shows progress and blocks a second submit while waiting', async () => {
    jest.mocked(authApi.login).mockReturnValue(new Promise(() => {}));
    const { user } = renderWithApp(<LoginPage />);

    await user.type(screen.getByLabelText('Email'), 'asha@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(screen.getByRole('button', { name: 'Signing in…' })).toBeDisabled();
  });

  it('trims the email before sending it', async () => {
    jest.mocked(authApi.login).mockReturnValue(new Promise(() => {}));
    const { user } = renderWithApp(<LoginPage />);

    await user.type(screen.getByLabelText('Email'), '  asha@example.com ');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(authApi.login).toHaveBeenCalledWith('asha@example.com', 'password123');
  });
});
