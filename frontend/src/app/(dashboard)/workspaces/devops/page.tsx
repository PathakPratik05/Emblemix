'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Badge } from '../../../../components/ui/Badge';
import { Button } from '../../../../components/ui/Button';
import Link from 'next/link';
import {
  Cpu,
  Server,
  Activity,
  AlertTriangle,
  CheckCircle2,
  GitBranch,
  ShieldAlert,
  ArrowRight,
  HardDrive,
  RefreshCw,
} from 'lucide-react';

export default function DevOpsWorkspacePage() {
  const { data: systems, isLoading: systemsLoading } = useQuery({
    queryKey: ['connected-systems'],
    queryFn: () => api.systems.getAll(),
  });

  const { data: incidents, isLoading: incidentsLoading } = useQuery({
    queryKey: ['incidents-devops'],
    queryFn: () => api.incidents.getAll(),
  });

  const { data: kpis } = useQuery({
    queryKey: ['analytics-kpis'],
    queryFn: () => api.analytics.getKpis(),
  });

  const infraSystems = (systems?.content || []).filter(
    (s) => s.systemType === 'INFRASTRUCTURE' || s.systemType === 'WEBHOOK' || s.environment === 'PRODUCTION'
  );

  const infraIncidents = (incidents?.content || []).filter(
    (i) => i.assignedTeamName?.toLowerCase().includes('devops') || i.severity === 'CRITICAL' || i.severity === 'HIGH'
  );

  return (
    <div className="space-y-6">
      {/* Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-purple-100 text-purple-700 rounded-lg">
              <Cpu className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">DevOps & SRE Workspace</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Centralized operations console for infrastructure availability, deployment health, container telemetry, and infra incidents.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className="text-xs bg-purple-50 text-purple-700 border-purple-200">
            Team: DevOps
          </Badge>
          <Link href="/systems">
            <Button size="sm" variant="outline" className="flex items-center space-x-1">
              <span>All Systems</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Button>
          </Link>
        </div>
      </div>

      {/* DevOps Health & Telemetry Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Infra Availability</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">
                {kpis?.availabilityPercentage ? `${kpis.availabilityPercentage}%` : '99.98%'}
              </p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <Activity className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Active Pods & Containers</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">48 / 48</p>
            </div>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-lg">
              <HardDrive className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Deployments Today</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">6 Passed</p>
            </div>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-lg">
              <GitBranch className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Infra Incidents</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{infraIncidents.length}</p>
            </div>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-lg">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Workspace Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monitored Infrastructure Systems */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Server className="w-4 h-4 text-purple-600" />
                <span>Infrastructure Systems & Clusters</span>
              </CardTitle>
              <span className="text-xs text-slate-400">{infraSystems.length} systems registered</span>
            </CardHeader>
            <CardContent className="p-0">
              {systemsLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading infrastructure...</div>
              ) : infraSystems.length === 0 ? (
                <div className="p-8 text-center text-slate-400 text-sm">No infrastructure systems assigned to DevOps.</div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {infraSystems.map((sys) => (
                    <div key={sys.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50 transition-colors">
                      <div className="min-w-0 pr-4">
                        <div className="flex items-center space-x-2">
                          <p className="text-sm font-semibold text-slate-900 truncate">{sys.name}</p>
                          <Badge variant="outline" className="text-[10px] uppercase font-bold">
                            {sys.environment}
                          </Badge>
                          {sys.criticality && (
                            <span className="text-[10px] font-bold text-red-700 bg-red-50 border border-red-200 px-1 rounded">
                              {sys.criticality}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5 truncate">
                          Owner: {sys.primaryOwnerName || 'DevOps Team'} • SLA: {sys.escalationPolicy || 'P1-15m'}
                        </p>
                      </div>
                      <div className="flex items-center space-x-3 flex-shrink-0">
                        <span
                          className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            sys.status === 'HEALTHY'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : sys.status === 'DEGRADED'
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-red-50 text-red-700 border border-red-200'
                          }`}
                        >
                          {sys.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Kubernetes & Cluster Telemetry Status */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <HardDrive className="w-4 h-4 text-blue-600" />
                <span>Kubernetes Cluster & Node Pools</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs">
                  <p className="font-semibold text-slate-600">Production EKS (us-east-1)</p>
                  <p className="text-slate-900 font-bold mt-1">12 Nodes • 99.99% Uptime</p>
                  <p className="text-emerald-600 text-[11px] mt-1 flex items-center space-x-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Control plane healthy</span>
                  </p>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs">
                  <p className="font-semibold text-slate-600">Staging GKE (us-central1)</p>
                  <p className="text-slate-900 font-bold mt-1">4 Nodes • 100% Uptime</p>
                  <p className="text-emerald-600 text-[11px] mt-1 flex items-center space-x-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>All ingress pods ready</span>
                  </p>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs">
                  <p className="font-semibold text-slate-600">Database Replica Pool</p>
                  <p className="text-slate-900 font-bold mt-1">3 Replicas • Sync Lag &lt; 2ms</p>
                  <p className="text-emerald-600 text-[11px] mt-1 flex items-center space-x-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>WAL replication active</span>
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* DevOps Incidents & On-Call Feed */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <ShieldAlert className="w-4 h-4 text-red-600" />
                <span>DevOps Active Incidents</span>
              </CardTitle>
              <Link href="/incidents" className="text-xs text-blue-600 hover:underline">
                View All
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {incidentsLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading incidents...</div>
              ) : infraIncidents.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs flex flex-col items-center">
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 mb-2" />
                  <span>No active infrastructure incidents</span>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {infraIncidents.slice(0, 5).map((inc) => (
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
                        System: {inc.systemName} • Status: {inc.status}
                      </p>
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* CI/CD & Deployment Feed */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <GitBranch className="w-4 h-4 text-purple-600" />
                <span>Deployment Pipeline Events</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-4 space-y-3">
              <div className="text-xs border-l-2 border-emerald-500 pl-3 py-0.5">
                <p className="font-semibold text-slate-900">v2.4.1 Production Deploy</p>
                <p className="text-slate-500 text-[11px]">Next.js Frontend & API Gateway • 45m ago</p>
                <span className="text-emerald-700 font-medium text-[10px]">SUCCESS</span>
              </div>
              <div className="text-xs border-l-2 border-blue-500 pl-3 py-0.5">
                <p className="font-semibold text-slate-900">Database Schema Migration V4</p>
                <p className="text-slate-500 text-[11px]">PostgreSQL Flyway Migration • 2h ago</p>
                <span className="text-blue-700 font-medium text-[10px]">APPLIED</span>
              </div>
              <div className="text-xs border-l-2 border-purple-500 pl-3 py-0.5">
                <p className="font-semibold text-slate-900">Container Security Scan</p>
                <p className="text-slate-500 text-[11px]">Trivy & Snyk vulnerability check • 4h ago</p>
                <span className="text-purple-700 font-medium text-[10px]">0 CRITICAL VULNS</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
