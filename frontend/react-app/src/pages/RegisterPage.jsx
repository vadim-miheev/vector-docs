import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    const emailTrimmed = String(email || '').trim();
    const passwordTrimmed = String(password || '').trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailTrimmed || !passwordTrimmed) {
      setError('Email and password are required');
      return;
    }
    if (!emailRegex.test(emailTrimmed)) {
      setError('Please enter a valid email address');
      return;
    }
    try {
      const result = await register(emailTrimmed, passwordTrimmed);
        if (result !== null) {
            navigate('/documents');
        }
    } catch (e) {
      setError(e.message || 'Registration failed');
    }
  }

  return (
    <div style={{ padding: 16 }}>
      <h2>Register</h2>
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 320 }}>
        <input placeholder="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <input placeholder="Password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">Create account</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
      </form>
      <p>Already have an account? <Link to="/login">Log in</Link></p>
    </div>
  );
}
