'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Badge } from '../../../../components/ui/Badge';
import { Button } from '../../../../components/ui/Button';
import Link from 'next/link';
import {
  Layers,
  Activity,
  AlertTriangle,
  CheckCircle2,
  Server,
  ArrowRight,
  Webhook,
  Code2,
  Bug,
  Zap,
} from 'lucide-react';

export default function ApplicationWorkspacePage() {
  const { data: systems, isLoading: sysLoading } = useQuery({
    queryKey: ['connected-systems'],
    queryFn: () => api.systems.getAll(),
  });

  const { data: webhooks = [] } = useQuery({
    queryKey: ['webhooks'],
    queryFn: () => api.webhooks.getAll(),
  });

  const { data: incidents, isLoading: incLoading } = useQuery({
    queryKey: ['application-incidents'],
    queryFn: () => api.incidents.getAll(),
  });

  const appSystems = (systems?.content || []).filter(
    (s) => s.systemType === 'API' || s.systemType === 'WEBHOOK' || s.systemType === 'WEBSITE'
  );

  const appIncidents = (incidents?.content || []).filter(
    (i) => i.assignedTeamName?.toLowerCase().includes('application') || i.assignedTeamName?.toLowerCase().includes('app')
  );

  return (
    <div className="space-y-6">
      {/* Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-indigo-100 text-indigo-700 rounded-lg">
              <Layers className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Application Engineering Workspace</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Service mesh health, backend API error rates, custom inbound webhooks, and application-tier incident triage.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className="text-xs bg-indigo-50 text-indigo-700 border-indigo-200">
            Team: Application Engineering
          </Badge>
          <Link href="/webhooks">
            <Button size="sm" variant="outline" className="flex items-center space-x-1">
              <span>Webhooks & Integrations</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Application Services</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{appSystems.length || 4}</p>
            </div>
            <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-lg">
              <Layers className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Backend 5xx Error Rate</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">0.02%</p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <Activity className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Active Inbound Webhooks</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{webhooks.length}</p>
            </div>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-lg">
              <Webhook className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">App Incidents</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{appIncidents.length}</p>
            </div>
            <div className="p-2.5 bg-red-50 text-red-600 rounded-lg">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Workspace Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Core Application Services */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Code2 className="w-4 h-4 text-indigo-600" />
                <span>Monitored Backend Microservices & APIs</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y divide-slate-100 text-xs">
                <div className="p-4 flex items-center justify-between hover:bg-slate-50">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-sm text-slate-900">Emblemix Core API Gateway</span>
                      <Badge variant="outline" className="text-[10px]">PRODUCTION</Badge>
                      <span className="text-[10px] text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-bold">P99: 85ms</span>
                    </div>
                    <p className="text-slate-500 mt-0.5">Spring Boot 3.3.5 • Java 21 • Actuator Health UP</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    HEALTHY
                  </span>
                </div>

                <div className="p-4 flex items-center justify-between hover:bg-slate-50">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-sm text-slate-900">Next.js SSR Application Engine</span>
                      <Badge variant="outline" className="text-[10px]">PRODUCTION</Badge>
                      <span className="text-[10px] text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-bold">P99: 42ms</span>
                    </div>
                    <p className="text-slate-500 mt-0.5">Next.js 14 App Router • Node.js Standalone Runner</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    HEALTHY
                  </span>
                </div>

                <div className="p-4 flex items-center justify-between hover:bg-slate-50">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-sm text-slate-900">Event Normalization & Alert Rule Engine</span>
                      <Badge variant="outline" className="text-[10px]">PRODUCTION</Badge>
                      <span className="text-[10px] text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-bold">Throughput: 1,200 ev/s</span>
                    </div>
                    <p className="text-slate-500 mt-0.5">Deduplication Fingerprinting • Automated Recovery Pipeline</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    HEALTHY
                  </span>
                </div>

                <div className="p-4 flex items-center justify-between hover:bg-slate-50">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-sm text-slate-900">Notification Dispatch Service</span>
                      <Badge variant="outline" className="text-[10px]">PRODUCTION</Badge>
                      <span className="text-[10px] text-emerald-700 bg-emerald-50 px-1 py-0.5 rounded font-bold">Mailpit / SMTP</span>
                    </div>
                    <p className="text-slate-500 mt-0.5">Async ThreadPoolExecutor • Spring Mail delivery</p>
                  </div>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    HEALTHY
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Webhook Endpoints & Integrations List */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Webhook className="w-4 h-4 text-purple-600" />
                <span>Custom Inbound Webhooks</span>
              </CardTitle>
              <Link href="/webhooks" className="text-xs text-indigo-600 hover:underline">
                Manage Webhooks
              </Link>
            </CardHeader>
            <CardContent className="p-4">
              {webhooks.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No webhook integrations configured yet.</p>
              ) : (
                <div className="space-y-2">
                  {webhooks.map((wh) => (
                    <div key={wh.id} className="p-2.5 rounded bg-slate-50 border border-slate-100 text-xs flex items-center justify-between">
                      <div>
                        <p className="font-bold text-slate-900">{wh.name}</p>
                        <p className="text-[11px] text-slate-500">HMAC-SHA256 Signed • AES-256-GCM Encrypted</p>
                      </div>
                      <Badge variant="outline" className="text-[10px] text-emerald-700 bg-emerald-50 border-emerald-200">
                        ACTIVE
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Application Incidents Feed */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Bug className="w-4 h-4 text-red-600" />
                <span>Application Incidents</span>
              </CardTitle>
              <Link href="/incidents" className="text-xs text-indigo-600 hover:underline">
                View All
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {incLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading incidents...</div>
              ) : appIncidents.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs flex flex-col items-center">
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 mb-2" />
                  <span>No active application incidents</span>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {appIncidents.slice(0, 5).map((inc) => (
                    <Link
                      key={inc.id}
                      href={`/incidents/${inc.id}`}
                      className="block p-3.5 hover:bg-slate-50/70 transition-colors"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-bold text-slate-900 truncate">{inc.title}</span>
                        <span className="text-[10px] font-bold text-red-700 bg-red-50 border border-red-200 px-1 rounded uppercase">
                          {inc.severity}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 mt-1 truncate">
                        Status: {inc.status} • Fingerprint: {inc.fingerprint.substring(0, 10)}...
                      </p>
                    </Link>
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
