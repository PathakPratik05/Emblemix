'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { api } from '../../../lib/api';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';

export default function ReportsPage() {
  const { data: uptimeData, isLoading } = useQuery({
    queryKey: ['uptime-reports'],
    queryFn: () => api.dashboard.getUptimeReports(30),
  });

  const handleExportCsv = () => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('emblemix_token') : null;
    fetch('/api/v1/reports/export/incidents.csv', {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
      .then((res) => res.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `emblemix_incidents_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
      })
      .catch((err) => alert('CSV export failed: ' + err.message));
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Reports & Analytics</h2>
          <p className="text-sm text-slate-500 mt-1">
            Uptime SLA tracking, latency performance, and compliance audit exports.
          </p>
        </div>
        <Button onClick={handleExportCsv} className="space-x-1.5 bg-emerald-600 hover:bg-emerald-700 text-white">
          <Download className="w-4 h-4" />
          <span>Export Incidents to CSV</span>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>System Availability & Latency (Last 30 Days)</CardTitle>
          <CardDescription>Calculated from live scheduled health checks</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">System Name</th>
                  <th className="py-3 px-6">Uptime Percentage</th>
                  <th className="py-3 px-6">Avg Latency (ms)</th>
                  <th className="py-3 px-6">Total Probes</th>
                  <th className="py-3 px-6">Successful</th>
                  <th className="py-3 px-6">Failed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-xs">
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-10 text-slate-400 font-sans">
                      Loading reporting data...
                    </td>
                  </tr>
                ) : uptimeData && uptimeData.length > 0 ? (
                  uptimeData.map((row) => (
                    <tr key={row.systemId} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900 font-sans text-sm">
                        {row.systemName}
                      </td>
                      <td className="py-4 px-6">
                        <span
                          className={`font-bold ${
                            row.uptimePercentage >= 99.0
                              ? 'text-emerald-600'
                              : row.uptimePercentage >= 95.0
                              ? 'text-amber-600'
                              : 'text-red-600'
                          }`}
                        >
                          {row.uptimePercentage}%
                        </span>
                      </td>
                      <td className="py-4 px-6 text-slate-800">{row.averageResponseTimeMs} ms</td>
                      <td className="py-4 px-6 text-slate-600">{row.totalChecks}</td>
                      <td className="py-4 px-6 text-emerald-600 font-semibold">{row.successfulChecks}</td>
                      <td className="py-4 px-6 text-red-600 font-semibold">{row.failedChecks}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-slate-400 font-sans">
                      No uptime telemetry captured yet.
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
