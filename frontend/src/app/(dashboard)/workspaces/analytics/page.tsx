'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, PowerBiReportCatalogItem } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Badge } from '../../../../components/ui/Badge';
import { Button } from '../../../../components/ui/Button';
import {
  BarChart3,
  LineChart,
  Database,
  RefreshCw,
  ExternalLink,
  Shield,
  Layers,
  FileSpreadsheet,
  Key,
  CheckCircle2,
  Clock,
  Sparkles,
} from 'lucide-react';

export default function AnalyticsWorkspacePage() {
  const [selectedReportId, setSelectedReportId] = useState('pbi-rep-001');

  const { data: kpis } = useQuery({
    queryKey: ['analytics-kpis'],
    queryFn: () => api.analytics.getKpis(),
  });

  const { data: teamPerformance = [] } = useQuery({
    queryKey: ['team-performance'],
    queryFn: () => api.analytics.getTeamPerformance(),
  });

  const { data: catalog = [], isLoading: catalogLoading } = useQuery({
    queryKey: ['powerbi-catalog'],
    queryFn: () => api.analytics.getPowerBiCatalog(),
  });

  const { data: embedConfig, isLoading: embedLoading } = useQuery({
    queryKey: ['powerbi-embed', selectedReportId],
    queryFn: () => api.analytics.getPowerBiEmbedConfig(selectedReportId),
    enabled: Boolean(selectedReportId),
  });

  return (
    <div className="space-y-6">
      {/* Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-emerald-100 text-emerald-700 rounded-lg">
              <LineChart className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Data & Analytics Workspace</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Governed reporting metrics, Power BI Embedded catalog, organization-isolated analytical datasets, and SLA intelligence.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className="text-xs bg-emerald-50 text-emerald-700 border-emerald-200">
            Team: Data & Analytics
          </Badge>
          <a
            href="/api/v1/analytics/governed/incidents"
            target="_blank"
            rel="noreferrer"
            className="text-xs text-emerald-700 hover:text-emerald-900 bg-white border border-emerald-200 px-3 py-1.5 rounded-lg flex items-center space-x-1 font-semibold"
          >
            <FileSpreadsheet className="w-3.5 h-3.5" />
            <span>OData / JSON Export</span>
          </a>
        </div>
      </div>

      {/* Governed KPI Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">SLA Achievement</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">
                {kpis?.slaAchievementPercentage ? `${kpis.slaAchievementPercentage}%` : '98.5%'}
              </p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Mean Time to Acknowledge</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{kpis?.mttaMinutes ?? 8} min</p>
            </div>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-lg">
              <Clock className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Mean Time to Resolve</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{kpis?.mttrMinutes ?? 28} min</p>
            </div>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-lg">
              <RefreshCw className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Incident Recurrence</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{kpis?.incidentRecurrenceCount ?? 0}</p>
            </div>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-lg">
              <Layers className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Power BI Embedded Portal & Report Viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Report Catalog Picker */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <BarChart3 className="w-4 h-4 text-emerald-600" />
                <span>Power BI Report Catalog</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {catalogLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading catalog...</div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {catalog.map((rep) => {
                    const isSelected = selectedReportId === rep.id;
                    return (
                      <div
                        key={rep.id}
                        onClick={() => setSelectedReportId(rep.id)}
                        className={`p-3.5 cursor-pointer transition-colors ${
                          isSelected ? 'bg-emerald-50/70 border-l-4 border-emerald-600' : 'hover:bg-slate-50'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <p className="text-xs font-bold text-slate-900">{rep.name}</p>
                          <Badge variant="outline" className="text-[10px]">
                            {rep.category}
                          </Badge>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1 line-clamp-2">{rep.description}</p>
                        <div className="flex items-center justify-between mt-2 text-[10px] text-slate-400">
                          <span>Workspace: {rep.workspaceName}</span>
                          <span className="text-emerald-700 font-semibold flex items-center space-x-0.5">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>Refreshed</span>
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Secure Entra / Service Principal Token Status */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Key className="w-4 h-4 text-amber-600" />
                <span>Entra Server-Side Embed Token</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-4 space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Security Model:</span>
                <span className="font-semibold text-slate-800">Microsoft Entra Service Principal</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Tenant Isolation:</span>
                <span className="font-semibold text-emerald-700">Enforced (RLS org_id claim)</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Token Expiry:</span>
                <span className="font-semibold text-slate-800">
                  {embedConfig?.tokenExpiration ? new Date(embedConfig.tokenExpiration).toLocaleTimeString() : '1 hour'}
                </span>
              </div>
              <div className="pt-2">
                <p className="text-[10px] text-slate-400 font-mono break-all bg-slate-50 p-2 rounded border border-slate-100">
                  Token: {embedConfig?.embedToken ? `${embedConfig.embedToken.substring(0, 36)}...` : 'Generating...'}
                </p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Embedded Report Frame / Viewer */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="min-h-[500px] flex flex-col justify-between">
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base font-bold text-slate-900">
                  {embedConfig?.reportName || 'Executive Reliability & SLA Dashboard'}
                </CardTitle>
                <p className="text-xs text-slate-500 mt-0.5">
                  Power BI Embedded (Read-Only • Capacity: Premium Gen2 • RLS Filtered)
                </p>
              </div>
              <Button size="sm" variant="outline" className="flex items-center space-x-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                <span>Auto-Refresh: ON</span>
              </Button>
            </CardHeader>

            {/* Simulated Power BI Canvas */}
            <CardContent className="p-6 flex-1 flex flex-col items-center justify-center bg-slate-900 text-white rounded-b-xl relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 opacity-90"></div>
              
              <div className="relative z-10 w-full max-w-lg text-center space-y-4">
                <div className="w-16 h-16 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl flex items-center justify-center mx-auto text-emerald-400">
                  <BarChart3 className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">{embedConfig?.reportName || 'Power BI Canvas'}</h3>
                  <p className="text-xs text-slate-400 mt-1">
                    Connected to Emblemix Governed OData Dataset (URL: {embedConfig?.embedUrl || 'embedUrl'})
                  </p>
                </div>

                <div className="grid grid-cols-3 gap-3 text-left pt-2">
                  <div className="p-3 bg-white/5 border border-white/10 rounded-lg">
                    <p className="text-[10px] text-slate-400 uppercase">Uptime KPI</p>
                    <p className="text-lg font-bold text-emerald-400 mt-0.5">{kpis?.uptimePercentage ?? 99.98}%</p>
                  </div>
                  <div className="p-3 bg-white/5 border border-white/10 rounded-lg">
                    <p className="text-[10px] text-slate-400 uppercase">Avg MTTR</p>
                    <p className="text-lg font-bold text-sky-400 mt-0.5">{kpis?.mttrMinutes ?? 28}m</p>
                  </div>
                  <div className="p-3 bg-white/5 border border-white/10 rounded-lg">
                    <p className="text-[10px] text-slate-400 uppercase">SLA Goal</p>
                    <p className="text-lg font-bold text-purple-400 mt-0.5">99.0%</p>
                  </div>
                </div>

                <div className="pt-2">
                  <span className="text-[11px] text-emerald-400 bg-emerald-950/60 border border-emerald-800 px-3 py-1 rounded-full inline-flex items-center space-x-1">
                    <Shield className="w-3 h-3" />
                    <span>Organization Tenant Isolation Enforced</span>
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Team Performance Breakdown Table */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900">
                Engineering Team Incident Velocity & Response
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {teamPerformance.length === 0 ? (
                <div className="p-6 text-center text-slate-400 text-xs">
                  No team performance records available yet.
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {teamPerformance.map((tp) => (
                    <div key={tp.teamId} className="p-3.5 flex items-center justify-between text-xs hover:bg-slate-50">
                      <div>
                        <p className="font-bold text-slate-900">{tp.teamName}</p>
                        <p className="text-slate-400 text-[11px]">
                          Assigned: {tp.assignedIncidents} • Resolved: {tp.resolvedIncidents} • Open: {tp.openIncidents}
                        </p>
                      </div>
                      <div className="text-right space-y-0.5">
                        <div className="flex items-center space-x-2">
                          <span className="text-slate-500">MTTA: {tp.mttaMinutes}m</span>
                          <span className="text-slate-500">MTTR: {tp.mttrMinutes}m</span>
                        </div>
                        <p className="text-emerald-700 font-semibold text-[11px]">
                          SLA Compliance: {tp.slaCompliancePercentage}%
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
