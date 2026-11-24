'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Server,
  Globe,
  Cloud,
  Webhook,
  Blocks,
  AlertTriangle,
  Bell,
  Sliders,
  BarChart3,
  Settings,
  LogOut,
  ShieldCheck,
  Users,
  Cpu,
  Layers,
  LineChart,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { cn } from '../../lib/utils';

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();
  const pathname = usePathname();

  const mainNav = [
    { label: 'Overview', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Connected Systems', path: '/systems', icon: Server },
    { label: 'Integrations Hub', path: '/integrations', icon: Blocks },
    { label: 'Incidents', path: '/incidents', icon: AlertTriangle },
  ];

  const teamNav = [
    { label: 'Teams & Ownership', path: '/teams', icon: Users },
    { label: 'DevOps Workspace', path: '/workspaces/devops', icon: Cpu },
    { label: 'Salesforce Workspace', path: '/workspaces/salesforce', icon: Cloud },
    { label: 'Web Engineering', path: '/workspaces/web', icon: Globe },
    { label: 'Data & Analytics', path: '/workspaces/analytics', icon: LineChart },
    { label: 'App Engineering', path: '/workspaces/application', icon: Layers },
  ];

  const adminNav = [
    { label: 'Alert Rules', path: '/alert-rules', icon: Sliders },
    { label: 'Notifications', path: '/notifications', icon: Bell },
    { label: 'Reports', path: '/reports', icon: BarChart3 },
    { label: 'Settings', path: '/settings', icon: Settings },
  ];

  return (
    <aside className="w-64 border-r border-slate-200 bg-white flex flex-col justify-between h-screen sticky top-0">
      <div>
        {/* Brand Header */}
        <div className="h-16 flex items-center px-6 border-b border-slate-100">
          <Link href="/dashboard" className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-lg tracking-tight text-slate-900">Emblemix</span>
              <span className="block text-[10px] font-semibold text-blue-600 uppercase tracking-wider">
                Universal Monitoring
              </span>
            </div>
          </Link>
        </div>

        {/* Navigation Links */}
        <nav className="p-3 space-y-4 overflow-y-auto max-h-[calc(100vh-140px)]">
          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Core</p>
            <div className="space-y-0.5">
              {mainNav.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.path || (item.path !== '/dashboard' && pathname?.startsWith(item.path));
                return (
                  <Link
                    key={item.path}
                    href={item.path}
                    className={cn(
                      'flex items-center space-x-3 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-blue-50 text-blue-700 font-semibold shadow-sm'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    )}
                  >
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>

          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Collaboration</p>
            <div className="space-y-0.5">
              {teamNav.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.path || pathname?.startsWith(item.path);
                return (
                  <Link
                    key={item.path}
                    href={item.path}
                    className={cn(
                      'flex items-center space-x-3 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-blue-50 text-blue-700 font-semibold shadow-sm'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    )}
                  >
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>

          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Management</p>
            <div className="space-y-0.5">
              {adminNav.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.path || pathname?.startsWith(item.path);
                return (
                  <Link
                    key={item.path}
                    href={item.path}
                    className={cn(
                      'flex items-center space-x-3 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-blue-50 text-blue-700 font-semibold shadow-sm'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    )}
                  >
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>
        </nav>
      </div>

      {/* User and Tenant Footer */}
      <div className="p-4 border-t border-slate-100 bg-slate-50/50">
        <div className="flex items-center justify-between">
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-slate-900 truncate">
              {user?.firstName ? `${user.firstName} ${user.lastName}` : 'Administrator'}
            </p>
            <p className="text-xs text-slate-500 truncate">{user?.organizationName || 'Emblemix Tenant'}</p>
            <span className="inline-block mt-1 text-[10px] uppercase font-bold text-blue-700 bg-blue-50 border border-blue-200 px-1.5 py-0.5 rounded">
              {user?.role || 'ENGINEER'}
            </span>
          </div>
          <button
            onClick={logout}
            title="Sign out"
            className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
