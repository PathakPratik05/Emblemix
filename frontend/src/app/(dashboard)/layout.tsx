'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Sidebar } from '../../components/layout/Sidebar';
import { Navbar } from '../../components/layout/Navbar';
import { useAuth } from '../../context/AuthContext';
import { Loader2 } from 'lucide-react';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace('/login');
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="text-center space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600 mx-auto" />
          <p className="text-sm text-slate-500 font-medium">Loading Emblemix Platform...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  const getPageTitle = (path: string | null) => {
    if (!path || path === '/dashboard') return 'Overview Dashboard';
    if (path.startsWith('/systems')) return 'Connected Systems';
    if (path.startsWith('/integrations')) return 'Integrations Hub';
    if (path.startsWith('/websites')) return 'Website Monitoring';
    if (path.startsWith('/salesforce')) return 'Salesforce Organizations';
    if (path.startsWith('/webhooks')) return 'Custom Webhook Integrations';
    if (path.startsWith('/incidents')) return 'Incident Management';
    if (path.startsWith('/alert-rules')) return 'Alert Rules Engine';
    if (path.startsWith('/notifications')) return 'Notification Channels';
    if (path.startsWith('/reports')) return 'Reports & Analytics';
    if (path.startsWith('/settings')) return 'Settings';
    return 'Emblemix Platform';
  };

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar title={getPageTitle(pathname)} />
        <main className="flex-1 p-8 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
