import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../services/authService';

export default function PasswordResetRequestPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    const emailTrimmed = String(email || '').trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailTrimmed) {
      setError('Email is required');
      return;
    }
    if (!emailRegex.test(emailTrimmed)) {
      setError('Please enter a valid email address');
      return;
    }
    try {
      setSubmitting(true);
      const res = await authService.passwordResetRequest({ email: emailTrimmed });
      if (res && !res.error) {
        setSuccess('If an account with this email exists, we have sent a link to reset your password.');
      } else if (res && res.error) {
        setError(res.error);
      }
    } catch (e) {
      setError(e.message || 'Request failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={"p-4 flex flex-col items-center mt-[15%]"}>
      <h2 className={"text-4xl font-bold mb-8"}>Reset your password</h2>
      <form onSubmit={onSubmit} className="flex flex-col gap-2 max-w-[320px] w-full mb-4">
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={submitting}
        />
        <button className={"border border-black rounded-md py-1 bg-gray-100"} type="submit" disabled={submitting}>
          {submitting ? 'Submitting...' : 'Send reset link'}
        </button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        {success && <div style={{ color: 'green' }}>{success}</div>}
      </form>
      <p>Remembered your password? <Link className={"underline"} to="/login">Log in</Link></p>
    </div>
  );
}
