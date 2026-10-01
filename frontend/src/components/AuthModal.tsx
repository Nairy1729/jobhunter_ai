import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { AuthResponse } from '../types';
import {
  LogIn,
  UserPlus,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Eye,
  EyeOff,
  Sparkles,
  ArrowRight,
  KeyRound,
  Mail,
  User,
} from 'lucide-react';

interface AuthModalProps {
  onSuccess: (authData: AuthResponse) => void;
  initialMode?: 'login' | 'register';
}

export const AuthModal: React.FC<AuthModalProps> = ({
  onSuccess,
  initialMode = 'login',
}) => {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode);

  // Form Fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');

  // UI state
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Fill seeded candidate credentials
  const fillDemoAccount = () => {
    setMode('login');
    setEmail('candidate@jobhunter.ai');
    setPassword('password123');
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Basic client validations
    if (!email.trim() || !password) {
      setError('Please provide all required fields.');
      return;
    }

    if (mode === 'register') {
      if (!firstName.trim() || !lastName.trim()) {
        setError('First name and last name are required.');
        return;
      }
      if (password.length < 6) {
        setError('Password must be at least 6 characters.');
        return;
      }
      if (password !== confirmPassword) {
        setError('Passwords do not match.');
        return;
      }
    }

    setLoading(true);

    try {
      let authResponse: AuthResponse;
      if (mode === 'login') {
        authResponse = await apiClient.auth.login(email.trim(), password);
        setSuccess('Authentication verified. Loading candidate profile...');
      } else {
        authResponse = await apiClient.auth.register({
          email: email.trim(),
          password,
          firstName: firstName.trim(),
          lastName: lastName.trim(),
        });
        setSuccess('Account created and verified. Initializing profile...');
      }

      // Store JWT token
      localStorage.setItem('jh_token', authResponse.token);

      setTimeout(() => {
        onSuccess(authResponse);
      }, 500);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        (mode === 'login' ? 'Invalid email or password.' : 'Registration failed. Email may already be in use.');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto p-6 sm:p-8 rounded-2xl bg-dark-900 border border-white/10 shadow-2xl backdrop-blur-2xl font-sans relative">
      {/* Brand Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-xl bg-dark-950 border border-signal-emerald/30 flex items-center justify-center text-signal-emerald shadow-inner">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="font-mono text-[10px] text-ink-muted uppercase tracking-widest flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-pulse" />
              <span>JOBHUNTER OS // ACCESS GATE</span>
            </div>
            <h2 className="text-lg font-light tracking-tight text-ink-primary">
              {mode === 'login' ? 'Sign In to Workspace' : 'Create Candidate Account'}
            </h2>
          </div>
        </div>
      </div>

      {/* Mode Switcher Tabs */}
      <div className="grid grid-cols-2 p-1 rounded-xl bg-dark-950 border border-white/[0.06] mb-6 font-mono text-xs">
        <button
          type="button"
          onClick={() => {
            setMode('login');
            setError(null);
            setSuccess(null);
          }}
          className={`py-2 rounded-lg font-medium transition flex items-center justify-center gap-2 ${
            mode === 'login'
              ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm'
              : 'text-ink-muted hover:text-ink-primary'
          }`}
        >
          <LogIn className="w-3.5 h-3.5 text-signal-emerald" />
          <span>SIGN IN</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setMode('register');
            setError(null);
            setSuccess(null);
          }}
          className={`py-2 rounded-lg font-medium transition flex items-center justify-center gap-2 ${
            mode === 'register'
              ? 'bg-white/[0.08] text-ink-primary border border-white/15 shadow-sm'
              : 'text-ink-muted hover:text-ink-primary'
          }`}
        >
          <UserPlus className="w-3.5 h-3.5 text-signal-cyan" />
          <span>SIGN UP</span>
        </button>
      </div>

      {/* Alert Notices */}
      {error && (
        <div className="mb-5 p-3 rounded-xl bg-signal-rose/10 border border-signal-rose/25 text-signal-rose text-xs font-mono flex items-start gap-2.5 animate-in fade-in">
          <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span className="leading-relaxed">{error}</span>
        </div>
      )}

      {success && (
        <div className="mb-5 p-3 rounded-xl bg-signal-emerald/10 border border-signal-emerald/25 text-signal-emerald text-xs font-mono flex items-start gap-2.5 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span className="leading-relaxed">{success}</span>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4 font-mono text-xs">
        {mode === 'register' && (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[10px] text-ink-muted uppercase tracking-widest mb-1.5">
                FIRST NAME
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-ink-muted" />
                <input
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="Alex"
                  required
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary font-sans text-xs transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] text-ink-muted uppercase tracking-widest mb-1.5">
                LAST NAME
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-ink-muted" />
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder="Chen"
                  required
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary font-sans text-xs transition"
                />
              </div>
            </div>
          </div>
        )}

        <div>
          <label className="block text-[10px] text-ink-muted uppercase tracking-widest mb-1.5">
            EMAIL IDENTIFIER
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-ink-muted" />
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="candidate@jobhunter.ai"
              required
              className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary font-mono text-xs transition"
            />
          </div>
        </div>

        <div>
          <label className="block text-[10px] text-ink-muted uppercase tracking-widest mb-1.5">
            {mode === 'login' ? 'PASSWORD' : 'CREATE PASSWORD (MIN 6 CHARS)'}
          </label>
          <div className="relative">
            <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-ink-muted" />
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              required
              minLength={6}
              className="w-full pl-9 pr-10 py-2.5 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary font-mono text-xs transition"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-muted hover:text-ink-primary transition"
              title={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
            </button>
          </div>
        </div>

        {mode === 'register' && (
          <div>
            <label className="block text-[10px] text-ink-muted uppercase tracking-widest mb-1.5">
              CONFIRM PASSWORD
            </label>
            <div className="relative">
              <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-ink-muted" />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••••••"
                required
                className="w-full pl-9 pr-10 py-2.5 rounded-xl bg-dark-950 border border-white/[0.08] focus:border-signal-emerald/60 focus:outline-none text-ink-primary font-mono text-xs transition"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-muted hover:text-ink-primary transition"
                title={showConfirmPassword ? 'Hide password' : 'Show password'}
              >
                {showConfirmPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              </button>
            </div>
            {password && confirmPassword && password !== confirmPassword && (
              <p className="text-[10px] text-signal-rose mt-1">Passwords do not match.</p>
            )}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full mt-2 py-3 rounded-xl bg-signal-emerald hover:bg-signal-emerald/90 disabled:opacity-50 text-dark-950 font-mono font-bold text-xs tracking-wider transition flex items-center justify-center gap-2 shadow-lg shadow-signal-emerald/20"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin text-dark-950" />
              <span>{mode === 'login' ? 'AUTHENTICATING...' : 'CREATING ACCOUNT...'}</span>
            </>
          ) : (
            <>
              <span>{mode === 'login' ? 'SIGN IN' : 'COMPLETE REGISTRATION'}</span>
              <ArrowRight className="w-3.5 h-3.5 text-dark-950" />
            </>
          )}
        </button>
      </form>

      {/* Quick Demo Credentials Footer */}
      <div className="mt-6 pt-4 border-t border-white/[0.06] flex items-center justify-between font-mono text-[10px]">
        <span className="text-ink-muted">TESTING CREDENTIALS:</span>
        <button
          type="button"
          onClick={fillDemoAccount}
          className="px-2.5 py-1 rounded bg-white/[0.04] hover:bg-white/[0.08] text-signal-emerald border border-signal-emerald/30 transition flex items-center gap-1.5"
        >
          <Sparkles className="w-3 h-3" />
          <span>USE SEEDED ACCOUNT</span>
        </button>
      </div>
    </div>
  );
};
