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
      <div className={"p-4 flex flex-col items-center mt-[15%]"}>
      <h2 className={"text-4xl font-bold mb-8"}>Register</h2>
      <form onSubmit={onSubmit} className="flex flex-col gap-2 max-w-[320px] w-full mb-4">
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button className={"border border-black rounded-md py-1 bg-gray-100"} type="submit">Create account</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
      </form>
      <p>Already have an account? <Link className={"underline"} to="/login">Log in</Link></p>
    </div>
  );
}
