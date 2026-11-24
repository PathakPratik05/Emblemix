'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Building, Users, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { api } from '../../../lib/api';
import { useAuth } from '../../../context/AuthContext';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../../components/ui/Card';
import { Badge } from '../../../components/ui/Badge';

export default function SettingsPage() {
  const { user } = useAuth();

  const { data: org } = useQuery({
    queryKey: ['current-organization'],
    queryFn: api.organization.getCurrent,
  });

  const { data: members } = useQuery({
    queryKey: ['organization-users'],
    queryFn: api.organization.getUsers,
  });

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Organization Settings</h2>
        <p className="text-sm text-slate-500 mt-1">
          Manage tenant configuration, members, and security preferences.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Organization Info */}
        <Card>
          <CardHeader className="flex flex-row items-center space-x-2">
            <Building className="w-5 h-5 text-blue-600" />
            <CardTitle className="text-base">Tenant Organization</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div>
              <span className="text-xs text-slate-500 block">Organization Name</span>
              <span className="font-semibold text-slate-900">{org?.name || user?.organizationName || 'Acme Global'}</span>
            </div>
            <div>
              <span className="text-xs text-slate-500 block">Tenant Slug</span>
              <span className="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-800">
                {org?.slug || 'acme-global'}
              </span>
            </div>
            <div>
              <span className="text-xs text-slate-500 block">Tenant ID (UUID)</span>
              <span className="font-mono text-xs text-slate-600 select-all">{user?.organizationId || 'default'}</span>
            </div>
          </CardContent>
        </Card>

        {/* User Profile */}
        <Card>
          <CardHeader className="flex flex-row items-center space-x-2">
            <ShieldCheck className="w-5 h-5 text-blue-600" />
            <CardTitle className="text-base">Current Account Profile</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div>
              <span className="text-xs text-slate-500 block">Name</span>
              <span className="font-semibold text-slate-900">
                {user?.firstName ? `${user.firstName} ${user.lastName}` : 'Administrator'}
              </span>
            </div>
            <div>
              <span className="text-xs text-slate-500 block">Email Address</span>
              <span className="font-mono text-xs text-slate-800">{user?.email || 'admin@emblemix.com'}</span>
            </div>
            <div>
              <span className="text-xs text-slate-500 block">Assigned Role</span>
              <Badge variant="info" className="mt-1">
                {user?.role || 'ORG_ADMIN'}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Organization Members Table */}
      <Card>
        <CardHeader className="flex flex-row items-center space-x-2">
          <Users className="w-5 h-5 text-blue-600" />
          <div>
            <CardTitle className="text-base">Organization Team Members</CardTitle>
            <CardDescription>Users authorized within this tenant partition</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <table className="w-full text-left text-sm border-collapse">
            <thead>
              <tr className="border-b bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                <th className="py-3 px-6">Name</th>
                <th className="py-3 px-6">Email</th>
                <th className="py-3 px-6">Role</th>
                <th className="py-3 px-6">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {members && members.length > 0 ? (
                members.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-50/50">
                    <td className="py-3 px-6 font-medium text-slate-900">
                      {m.firstName} {m.lastName}
                    </td>
                    <td className="py-3 px-6 text-slate-600 font-mono text-xs">{m.email}</td>
                    <td className="py-3 px-6">
                      <Badge variant="default">{m.role}</Badge>
                    </td>
                    <td className="py-3 px-6 text-emerald-600 font-medium text-xs flex items-center space-x-1">
                      <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                      <span>Active</span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-slate-400">
                    Loading team members...
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}
