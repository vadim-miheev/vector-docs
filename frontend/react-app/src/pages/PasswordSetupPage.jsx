import React, { useMemo, useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { authService } from '../services/authService';

export default function PasswordSetupPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('token') || '';
  }, [location.search]);

  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!token) {
      setError('Missing or invalid token.');
      return;
    }
    const pwd = String(password || '').trim();
    const pwd2 = String(password2 || '').trim();
    if (pwd.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    if (pwd !== pwd2) {
      setError('Passwords do not match');
      return;
    }
    try {
      setSubmitting(true);
      const res = await authService.passwordSetup({ token, password: pwd });
      if (res && !res.error) {
        setSuccess('Password has been set. You can now log in.');
        setTimeout(() => navigate('/login'), 1200);
      } else if (res && res.error) {
        setError(res.error);
      }
    } catch (e) {
      setError(e.message || 'Failed to set password');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={"p-4 flex flex-col items-center mt-[15%]"}>
      <h2 className={"text-4xl font-bold mb-8"}>Set your password</h2>
      <form onSubmit={onSubmit} className="flex flex-col gap-2 max-w-[320px] w-full mb-4">
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="New password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={submitting}
        />
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Repeat password"
          type="password"
          required
          value={password2}
          onChange={(e) => setPassword2(e.target.value)}
          disabled={submitting}
        />
        <button className={"border border-black rounded-md py-1 bg-gray-100"} type="submit" disabled={submitting || !token}>
          {submitting ? 'Saving...' : 'Save password'}
        </button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        {success && <div style={{ color: 'green' }}>{success}</div>}
        {!token && (
          <div style={{ color: 'red' }}>Token is missing. Please open the link from your email.</div>
        )}
      </form>
      <p>Remembered your password? <Link className={"underline"} to="/login">Log in</Link></p>
    </div>
  );
}
