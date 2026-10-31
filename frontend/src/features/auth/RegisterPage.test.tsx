import { screen, waitFor } from '@testing-library/react';
import * as authApi from '../../api/auth';
import { ApiError } from '../../api/client';
import { loadSession } from '../../auth/sessionStorage';
import { asha, renderWithApp } from '../../test/helpers';
import { RegisterPage } from './RegisterPage';

jest.mock('../../api/auth');

async function fillIn(user: ReturnType<typeof renderWithApp>['user'], password = 'password123') {
  await user.type(screen.getByLabelText('Name'), 'Asha Rao');
  await user.type(screen.getByLabelText('Email'), 'asha@example.com');
  await user.type(screen.getByLabelText('Password'), password);
}

describe('RegisterPage', () => {
  it('validates every field before calling the server', async () => {
    const { user } = renderWithApp(<RegisterPage />);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Name is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
  });

  it('requires a password of at least 8 characters', async () => {
    const { user } = renderWithApp(<RegisterPage />);

    await fillIn(user, 'short');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Password must be between 8 and 72 characters')).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
  });

  it('registers and then logs the new user straight in', async () => {
    jest.mocked(authApi.register).mockResolvedValue(asha);
    jest.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      user: asha,
    });
    const { user } = renderWithApp(<RegisterPage />);

    await fillIn(user);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => expect(loadSession()?.accessToken).toBe('token'));
    expect(authApi.register).toHaveBeenCalledWith('Asha Rao', 'asha@example.com', 'password123');
  });

  it('shows a duplicate email next to the email field', async () => {
    jest.mocked(authApi.register).mockRejectedValue(
      new ApiError(409, 'EMAIL_ALREADY_REGISTERED', 'This email is already registered', {
        email: 'This email is already registered',
      }),
    );
    const { user } = renderWithApp(<RegisterPage />);

    await fillIn(user);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('This email is already registered')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInvalid();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
