import React from 'react';
import { ShieldCheck, Activity } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Navbar: React.FC<{ title?: string }> = ({ title = 'Emblemix Platform' }) => {
  const { user } = useAuth();

  return (
    <header className="h-16 border-b border-slate-200 bg-white px-8 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center space-x-4">
        <h1 className="text-xl font-bold text-slate-900">{title}</h1>
        <div className="hidden md:flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-xs text-emerald-700 font-medium">
          <Activity className="w-3.5 h-3.5 animate-pulse" />
          <span>Active Ingestion Engine</span>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="text-right">
          <p className="text-sm font-medium text-slate-800">{user?.email}</p>
          <p className="text-xs text-slate-500 font-mono">Org ID: {user?.organizationId.substring(0, 8)}...</p>
        </div>
        <div className="w-9 h-9 rounded-full bg-slate-200 text-slate-700 font-bold flex items-center justify-center text-sm border border-slate-300">
          {user?.firstName?.charAt(0) || 'U'}
        </div>
      </div>
    </header>
  );
};
