import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function RegisterPage() {
  const { register } = useAuth();
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
      const res = await register(emailTrimmed);
      if (res !== null) {
        setSuccess('We\'ve sent you an email with a link to set your password.');
      }
    } catch (e) {
      setError(e.message || 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
      <div className={"p-4 flex flex-col items-center mt-[15%]"}>
      <h1 className={"text-4xl font-bold mb-8"}>Register</h1>
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
          {submitting ? 'Submitting...' : 'Create account'}
        </button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
        {success && <div style={{ color: 'green' }}>{success}</div>}
      </form>
      <p>Already have an account? <Link className={"underline"} to="/login">Log in</Link></p>
    </div>
  );
}
