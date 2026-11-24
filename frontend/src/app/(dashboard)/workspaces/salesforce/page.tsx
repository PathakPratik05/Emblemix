'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Badge } from '../../../../components/ui/Badge';
import { Button } from '../../../../components/ui/Button';
import Link from 'next/link';
import {
  Cloud,
  Activity,
  AlertTriangle,
  CheckCircle2,
  Cpu,
  ArrowRight,
  Database,
  RefreshCw,
  ExternalLink,
} from 'lucide-react';

export default function SalesforceWorkspacePage() {
  const { data: sfConfigs = [], isLoading: sfLoading } = useQuery({
    queryKey: ['salesforce-configs'],
    queryFn: () => api.salesforce.getAll(),
  });

  const { data: incidents, isLoading: incLoading } = useQuery({
    queryKey: ['salesforce-incidents'],
    queryFn: () => api.incidents.getAll(),
  });

  const sfIncidents = (incidents?.content || []).filter(
    (i) => i.systemName?.toLowerCase().includes('salesforce') || i.assignedTeamName?.toLowerCase().includes('salesforce')
  );

  const prodOrgs = sfConfigs.filter((c) => !c.isSandbox);
  const sandboxOrgs = sfConfigs.filter((c) => c.isSandbox);

  return (
    <div className="space-y-6">
      {/* Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-sky-100 text-sky-700 rounded-lg">
              <Cloud className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Salesforce Engineering Workspace</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Dedicated monitoring for Salesforce Production & Sandbox organizations, API quotas, Apex jobs, and Governor Limits.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className="text-xs bg-sky-50 text-sky-700 border-sky-200">
            Team: Salesforce
          </Badge>
          <Link href="/salesforce">
            <Button size="sm" variant="outline" className="flex items-center space-x-1">
              <span>Manage Orgs</span>
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
              <p className="text-xs font-semibold text-slate-500 uppercase">Connected Orgs</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{sfConfigs.length}</p>
            </div>
            <div className="p-2.5 bg-sky-50 text-sky-600 rounded-lg">
              <Cloud className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Production Environments</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{prodOrgs.length}</p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <Database className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Sandbox / UAT</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{sandboxOrgs.length}</p>
            </div>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-lg">
              <Activity className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Salesforce Incidents</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{sfIncidents.length}</p>
            </div>
            <div className="p-2.5 bg-red-50 text-red-600 rounded-lg">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Workspace Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Salesforce Connected Orgs */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Cloud className="w-4 h-4 text-sky-600" />
                <span>Salesforce Organizations & Connection Health</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {sfLoading ? (
                <div className="p-8 text-center text-slate-400 text-sm">Loading Salesforce organizations...</div>
              ) : sfConfigs.length === 0 ? (
                <div className="p-8 text-center text-slate-400 text-sm">
                  No Salesforce organizations connected yet.{' '}
                  <Link href="/salesforce" className="text-sky-600 hover:underline">
                    Connect an org
                  </Link>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {sfConfigs.map((org) => (
                    <div key={org.id} className="p-4 hover:bg-slate-50/50 transition-colors space-y-3">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-bold text-sm text-slate-900">{org.name}</span>
                            <Badge
                              variant="outline"
                              className={
                                org.isSandbox
                                  ? 'bg-amber-50 text-amber-700 border-amber-200'
                                  : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              }
                            >
                              {org.isSandbox ? 'SANDBOX' : 'PRODUCTION'}
                            </Badge>
                          </div>
                          <p className="text-xs text-slate-500 mt-0.5">
                            {org.instanceUrl || 'https://login.salesforce.com'} • Org ID: {org.externalOrgId || '00D...'}
                          </p>
                        </div>
                        <span
                          className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            org.status === 'HEALTHY'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-amber-50 text-amber-700 border border-amber-200'
                          }`}
                        >
                          {org.status}
                        </span>
                      </div>

                      {/* API Usage & Limits Bar */}
                      <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-100 space-y-1.5">
                        <div className="flex justify-between text-xs text-slate-600">
                          <span>API Request Quota Consumption</span>
                          <span className="font-semibold text-slate-800">42% (42,000 / 100,000)</span>
                        </div>
                        <div className="w-full bg-slate-200 rounded-full h-2">
                          <div className="bg-sky-500 h-2 rounded-full" style={{ width: '42%' }}></div>
                        </div>
                        <div className="flex justify-between text-[11px] text-slate-500 pt-1">
                          <span>Warning Threshold: {org.apiUsageWarningThresholdPct}%</span>
                          <span>Critical Threshold: {org.apiUsageCriticalThresholdPct}%</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Apex Jobs & Governor Limits Feed */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Cpu className="w-4 h-4 text-purple-600" />
                <span>Recent Apex Asynchronous Jobs & Health</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-4">
              <div className="space-y-2.5 text-xs">
                <div className="flex items-center justify-between p-2 rounded bg-slate-50 border border-slate-100">
                  <div>
                    <p className="font-semibold text-slate-900">BillingBatchSyncJob (Batch Apex)</p>
                    <p className="text-slate-500 text-[11px]">Processed 12,400 records • 0 failures</p>
                  </div>
                  <span className="text-emerald-700 font-bold text-[10px] bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded">
                    COMPLETED
                  </span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-50 border border-slate-100">
                  <div>
                    <p className="font-semibold text-slate-900">OpportunityRollupQueueable (Queueable Apex)</p>
                    <p className="text-slate-500 text-[11px]">Job ID: 7078Z000084uK9a • Execution time: 140ms</p>
                  </div>
                  <span className="text-emerald-700 font-bold text-[10px] bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded">
                    COMPLETED
                  </span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-50 border border-slate-100">
                  <div>
                    <p className="font-semibold text-slate-900">OrderProcessingSchedule (Scheduled Apex)</p>
                    <p className="text-slate-500 text-[11px]">Next execution: in 25 minutes</p>
                  </div>
                  <span className="text-blue-700 font-bold text-[10px] bg-blue-50 border border-blue-200 px-1.5 py-0.5 rounded">
                    QUEUED
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Salesforce Incidents */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-red-600" />
                <span>Salesforce Incidents</span>
              </CardTitle>
              <Link href="/incidents" className="text-xs text-sky-600 hover:underline">
                View All
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {incLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading incidents...</div>
              ) : sfIncidents.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs flex flex-col items-center">
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 mb-2" />
                  <span>No active Salesforce incidents</span>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {sfIncidents.slice(0, 5).map((inc) => (
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
                        Status: {inc.status} • Team: {inc.assignedTeamName || 'Salesforce'}
                      </p>
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Integration Health Card */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900">OAuth & Connected App</CardTitle>
            </CardHeader>
            <CardContent className="p-4 space-y-2 text-xs">
              <p className="text-slate-600">
                Connected App uses JWT Bearer Flow & OAuth 2.0 Web Server Flow. Access tokens are automatically refreshed before expiry.
              </p>
              <div className="pt-2">
                <Link
                  href="/salesforce"
                  className="text-sky-600 font-semibold hover:underline flex items-center space-x-1"
                >
                  <span>OAuth Configuration & Scopes</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
