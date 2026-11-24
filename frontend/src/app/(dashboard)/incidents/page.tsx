'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { AlertTriangle, CheckCircle2, Filter, Eye } from 'lucide-react';
import { api, Incident } from '../../../lib/api';
import { Card, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';

export default function IncidentsPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [severityFilter, setSeverityFilter] = useState<string>('');

  const { data: incidentsResponse, isLoading } = useQuery({
    queryKey: ['incidents-list', statusFilter, severityFilter],
    queryFn: () =>
      api.incidents.getAll({
        status: statusFilter || undefined,
        severity: severityFilter || undefined,
      }),
    refetchInterval: 15000,
  });

  const acknowledgeMutation = useMutation({
    mutationFn: (id: string) => api.incidents.acknowledge(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents-list'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-overview'] });
    },
  });

  const statuses = [
    { label: 'All Incidents', value: '' },
    { label: 'Open', value: 'OPEN' },
    { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
    { label: 'Resolved', value: 'RESOLVED' },
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Incident Management</h2>
          <p className="text-sm text-slate-500 mt-1">
            Track, assign, acknowledge, and resolve active alerts and system failures.
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center space-x-3">
          <div className="flex items-center bg-white border border-slate-200 rounded-lg p-1 space-x-1 text-xs">
            {statuses.map((st) => (
              <button
                key={st.value}
                onClick={() => setStatusFilter(st.value)}
                className={`px-3 py-1.5 rounded-md font-medium transition-colors ${
                  statusFilter === st.value
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {st.label}
              </button>
            ))}
          </div>

          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Severity</th>
                  <th className="py-3 px-6">System</th>
                  <th className="py-3 px-6">Incident Title</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6">Assigned To</th>
                  <th className="py-3 px-6">Triggered</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-10 text-slate-400">
                      Loading incidents...
                    </td>
                  </tr>
                ) : incidentsResponse?.content && incidentsResponse.content.length > 0 ? (
                  incidentsResponse.content.map((inc) => (
                    <tr key={inc.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6">
                        <Badge
                          variant={
                            inc.severity === 'CRITICAL'
                              ? 'destructive'
                              : inc.severity === 'HIGH'
                              ? 'warning'
                              : inc.severity === 'MEDIUM'
                              ? 'info'
                              : 'default'
                          }
                        >
                          {inc.severity}
                        </Badge>
                      </td>
                      <td className="py-4 px-6 font-semibold text-slate-900">{inc.systemName}</td>
                      <td className="py-4 px-6 max-w-sm truncate text-slate-800">{inc.title}</td>
                      <td className="py-4 px-6">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${
                            inc.status === 'OPEN'
                              ? 'bg-red-100 text-red-800'
                              : inc.status === 'ACKNOWLEDGED'
                              ? 'bg-amber-100 text-amber-800'
                              : inc.status === 'RESOLVED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {inc.status}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-600">
                        {inc.assignedUserName || <span className="text-slate-400 italic">Unassigned</span>}
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-500">
                        {new Date(inc.createdAt).toLocaleString()}
                      </td>
                      <td className="py-4 px-6 text-right space-x-2">
                        {inc.status === 'OPEN' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => acknowledgeMutation.mutate(inc.id)}
                            isLoading={acknowledgeMutation.isPending && acknowledgeMutation.variables === inc.id}
                          >
                            Acknowledge
                          </Button>
                        )}
                        <Link href={`/incidents/${inc.id}`}>
                          <Button variant="ghost" size="sm" className="space-x-1">
                            <Eye className="w-3.5 h-3.5" />
                            <span>Details</span>
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="text-center py-12 text-slate-400">
                      No incidents matching criteria. All systems operational.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
