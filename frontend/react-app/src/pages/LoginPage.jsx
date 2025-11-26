import React, {useState, useEffect} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useAuthContext} from '../store/AuthContext';
import {useAuth} from '../hooks/useAuth';
import { isDemoEnabled, fetchDemoJson } from '../services/demoService';

export default function LoginPage() {
  const { login } = useAuth();
  const { setUser } = useAuthContext();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  // Demo hint logic: show demo credentials when demo is enabled
  const showDemoHint = isDemoEnabled();
  const [demoCreds, setDemoCreds] = useState(null);

  useEffect(() => {
    if (!showDemoHint) return;
    let cancelled = false;
    fetchDemoJson()
      .then((data) => {
        if (!cancelled) {
          setDemoCreds({
            email: data?.demoUserEmail || '',
            password: data?.demoUserPassword || '',
          });
        }
      })
      .catch(() => {
        // ignore errors silently; hint just won't show credentials
      });
    return () => {
      cancelled = true;
    };
  }, [showDemoHint]);

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
    <div className={"p-4 flex flex-col items-center mt-[15%]"}>
      <h2 className={"text-4xl font-bold mb-8"}>Log in</h2>
      <form onSubmit={onSubmit} className="flex flex-col gap-2 max-w-[320px] w-full mb-4">
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Email"
          type="email" required
          value={email} onChange={(e) => setEmail(e.target.value)}
        />
        <input
          className={"border border-black rounded-md px-2 py-1"}
          placeholder="Password"
          type="password"
          required value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button className={"border border-black rounded-md py-1 bg-gray-100"} type="submit">Log in</button>
        {error && <div style={{ color: 'red' }}>{error}</div>}
      </form>
      {showDemoHint && (
        <div className={"mb-4 py-2 px-3 border border-blue-500 bg-blue-50 text-blue-800 rounded max-w-[320px] w-full"}>
          <div className="font-bold mb-1">Demo account</div>
          {demoCreds ? (
            <div className="text-sm">
              <div>Email: <span className="font-mono">{demoCreds.email}</span></div>
              <div>Password: <span className="font-mono">{demoCreds.password}</span></div>
            </div>
          ) : (
            <div className="text-sm">Loading demo credentials…</div>
          )}
        </div>
      )}
      <div className={"flex flex-col items-center gap-2"}>
        <p>No account? <Link className={"underline"} to="/register">Register</Link></p>
        <p>
          <Link className={"underline"} to="/password-reset">Reset password</Link>
        </p>
      </div>
    </div>
  );
}
