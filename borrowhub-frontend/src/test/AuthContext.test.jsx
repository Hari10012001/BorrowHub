import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import api from '../api/axios';

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

const TestConsumer = () => {
  const { user, isAuthenticated, login, logout, loading } = useAuth();
  if (loading) return <div>Loading Auth...</div>;

  return (
    <div>
      <span data-testid="auth-status">{isAuthenticated ? 'Authenticated' : 'Guest'}</span>
      {user && <span data-testid="username">{user.username}</span>}
      <button
        onClick={() => login('testuser', 'Password@123')}
        data-testid="login-btn"
      >
        Login
      </button>
      <button onClick={logout} data-testid="logout-btn">
        Logout
      </button>
    </div>
  );
};

describe('AuthContext and Mock API Tests', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    api.get.mockResolvedValue({
      data: {
        success: true,
        data: { id: 3, username: 'student1', email: 'student1@borrowhub.edu', role: 'STUDENT' },
      },
    });
  });

  it('should initialize with Guest state when no stored token exists', () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Guest');
  });

  it('should authenticate user and store token on successful login', async () => {
    api.post.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          token: 'mock-jwt-token-12345',
          username: 'student1',
          role: 'STUDENT',
        },
      },
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await act(async () => {
      screen.getByTestId('login-btn').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
      expect(screen.getByTestId('username')).toHaveTextContent('student1');
      expect(localStorage.getItem('borrowhub_token')).toBe('mock-jwt-token-12345');
    });
  });

  it('should clear state and remove token from localStorage on logout', async () => {
    localStorage.setItem('borrowhub_token', 'active-token');
    localStorage.setItem(
      'borrowhub_user',
      JSON.stringify({ id: 3, username: 'student1', role: 'STUDENT' })
    );

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('logout-btn')).toBeInTheDocument();
    });

    act(() => {
      screen.getByTestId('logout-btn').click();
    });

    expect(screen.getByTestId('auth-status')).toHaveTextContent('Guest');
    expect(localStorage.getItem('borrowhub_token')).toBeNull();
    expect(localStorage.getItem('borrowhub_user')).toBeNull();
  });
});
