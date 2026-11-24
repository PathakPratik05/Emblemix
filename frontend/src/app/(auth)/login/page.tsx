'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AlertCircle } from 'lucide-react';
import { useAuth } from '../../../context/AuthContext';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../../components/ui/Card';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      router.push('/dashboard');
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
              <Link href="/forgot-password" className="text-xs text-blue-600 hover:underline">
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
            <button
              type="button"
              onClick={() => handleDemoFill('viewer@emblemix.com')}
              className="px-2 py-1 bg-slate-100 hover:bg-slate-200 rounded text-slate-700 font-mono transition-colors"
            >
              viewer@emblemix.com
            </button>
          </div>
        </div>

        <div className="mt-6 text-center text-sm text-slate-500">
          Need a new organization?{' '}
          <Link href="/register" className="text-blue-600 hover:underline font-medium">
            Register Organization
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
