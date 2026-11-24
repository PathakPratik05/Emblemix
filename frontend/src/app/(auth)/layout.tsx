'use client';

import React from 'react';
import Link from 'next/link';
import { ShieldCheck } from 'lucide-react';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <Link href="/login" className="inline-flex flex-col items-center">
            <div className="w-12 h-12 bg-blue-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-blue-500/25">
              <ShieldCheck className="w-7 h-7" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-slate-900 mt-2">Emblemix</h2>
          </Link>
          <p className="text-sm text-slate-500">Universal Incident Management & Monitoring Platform</p>
        </div>
        {children}
      </div>
    </div>
  );
}
