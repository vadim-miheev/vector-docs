import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthContext } from '../store/AuthContext';
import { authService } from '../services/authService';
import { useAuth } from '../hooks/useAuth';

export default function LoginPage() {
  const { login } = useAuth();
  const { setUser } = useAuthContext();
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
      const u = await login(emailTrimmed, passwordTrimmed );
      setUser(u);
      if (u !== null) {
        navigate('/documents');
      }
    } catch (e) {
      setError(e.message || 'Login failed');
    }
  }

  return (
    <div style={{ padding: 16, display: "flex", flexDirection: "column", alignItems: "center" }}>
      <h2>Log in</h2>
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 320, width: '100%' }}>
        <input placeholder="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <input placeholder="Password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">Log in</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
      </form>
      <p>No account? <Link to="/register">Register</Link></p>
    </div>
  );
}
