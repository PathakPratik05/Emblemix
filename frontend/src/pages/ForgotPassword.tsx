import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, CheckCircle2, AlertCircle } from 'lucide-react';
import { api } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/Card';

export const ForgotPassword: React.FC = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [resetToken, setResetToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.auth.forgotPassword(email);
      if (res.resetToken) {
        setResetToken(res.resetToken);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to process password reset');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center text-white mx-auto shadow-lg shadow-blue-500/25">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Reset Password</h2>
          <p className="text-sm text-slate-500">Enter your email to receive recovery instructions</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Password Recovery</CardTitle>
            <CardDescription>Enter the email address tied to your account</CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {resetToken ? (
              <div className="space-y-4">
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-start space-x-2">
                  <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5 text-emerald-600" />
                  <div>
                    <p className="font-semibold">Reset token generated successfully!</p>
                    <p className="text-xs mt-1">In local development, your reset token is:</p>
                    <p className="font-mono bg-white p-1.5 mt-1 rounded border border-emerald-200 break-all select-all">
                      {resetToken}
                    </p>
                  </div>
                </div>
                <Link to={`/reset-password?token=${resetToken}`}>
                  <Button className="w-full">Proceed to Set New Password</Button>
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <Input
                  label="Email address"
                  type="email"
                  placeholder="you@company.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
                <Button type="submit" className="w-full" isLoading={loading}>
                  Send Reset Link
                </Button>
              </form>
            )}

            <p className="mt-6 text-center text-sm text-slate-500">
              Remembered your credentials?{' '}
              <Link to="/login" className="font-medium text-blue-600 hover:underline">
                Return to sign in
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
