import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/Card';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate('/');
    } catch (err: any) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoFill = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('Password123!');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center text-white mx-auto shadow-lg shadow-blue-500/25">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Sign in to Emblemix</h2>
          <p className="text-sm text-slate-500">Universal Incident Management & Monitoring Platform</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Account Login</CardTitle>
            <CardDescription>Enter your organization email and password</CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="Email address"
                type="email"
                placeholder="you@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-sm font-medium text-slate-700">Password</label>
                  <Link to="/forgot-password" className="text-xs text-blue-600 hover:underline">
                    Forgot password?
                  </Link>
                </div>
                <Input
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              <Button type="submit" className="w-full" isLoading={loading}>
                Sign in
              </Button>
            </form>

            {/* Quick Dev Seed Credentials helper */}
            <div className="mt-6 pt-4 border-t border-slate-100 text-xs">
              <p className="font-semibold text-slate-600 mb-2">Development Demo Accounts (password: Password123!):</p>
              <div className="flex flex-wrap gap-1.5">
                <button
                  type="button"
                  onClick={() => handleDemoFill('admin@emblemix.com')}
                  className="px-2 py-1 bg-slate-100 hover:bg-slate-200 rounded text-slate-700 font-mono transition-colors"
                >
                  admin@emblemix.com (Org Admin)
                </button>
                <button
                  type="button"
                  onClick={() => handleDemoFill('engineer@emblemix.com')}
                  className="px-2 py-1 bg-slate-100 hover:bg-slate-200 rounded text-slate-700 font-mono transition-colors"
                >
                  engineer@emblemix.com
                </button>
              </div>
            </div>

            <p className="mt-6 text-center text-sm text-slate-500">
              Don't have an organization account?{' '}
              <Link to="/register" className="font-medium text-blue-600 hover:underline">
                Register here
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
