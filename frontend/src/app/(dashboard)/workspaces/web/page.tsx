'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Badge } from '../../../../components/ui/Badge';
import { Button } from '../../../../components/ui/Button';
import Link from 'next/link';
import {
  Globe,
  Clock,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  ExternalLink,
  Activity,
  Lock,
} from 'lucide-react';

export default function WebWorkspacePage() {
  const { data: websites, isLoading: webLoading } = useQuery({
    queryKey: ['websites'],
    queryFn: () => api.websites.getAll(),
  });

  const { data: incidents, isLoading: incLoading } = useQuery({
    queryKey: ['web-incidents'],
    queryFn: () => api.incidents.getAll(),
  });

  const websiteList = websites?.content || [];
  const webIncidents = (incidents?.content || []).filter(
    (i) => i.title?.toLowerCase().includes('website') || i.assignedTeamName?.toLowerCase().includes('web')
  );

  const healthyCount = websiteList.filter((w) => w.status === 'HEALTHY').length;

  return (
    <div className="space-y-6">
      {/* Workspace Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-blue-100 text-blue-700 rounded-lg">
              <Globe className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Web Engineering Workspace</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Real-time HTTP/HTTPS endpoint availability, SSL certificate validity, response time latency, and website incidents.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant="outline" className="text-xs bg-blue-50 text-blue-700 border-blue-200">
            Team: Web Engineering
          </Badge>
          <Link href="/websites">
            <Button size="sm" variant="outline" className="flex items-center space-x-1">
              <span>Manage Endpoints</span>
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
              <p className="text-xs font-semibold text-slate-500 uppercase">Monitored Endpoints</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{websiteList.length}</p>
            </div>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-lg">
              <Globe className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Average Latency</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">142 ms</p>
            </div>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-lg">
              <Clock className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">SSL Certificates</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">100% Valid</p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <Lock className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Web Incidents</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{webIncidents.length}</p>
            </div>
            <div className="p-2.5 bg-red-50 text-red-600 rounded-lg">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Workspace Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monitored Websites & APIs */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <Globe className="w-4 h-4 text-blue-600" />
                <span>Endpoints, Response Latencies & SSL</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {webLoading ? (
                <div className="p-8 text-center text-slate-400 text-sm">Loading websites...</div>
              ) : websiteList.length === 0 ? (
                <div className="p-8 text-center text-slate-400 text-sm">
                  No websites registered yet.{' '}
                  <Link href="/websites" className="text-blue-600 hover:underline">
                    Register a website
                  </Link>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {websiteList.map((site) => (
                    <div key={site.id} className="p-4 hover:bg-slate-50/50 transition-colors flex items-center justify-between">
                      <div className="min-w-0 pr-4">
                        <div className="flex items-center space-x-2">
                          <p className="font-bold text-sm text-slate-900 truncate">{site.name}</p>
                          <Badge variant="outline" className="text-[10px] uppercase font-bold">
                            {site.environment}
                          </Badge>
                          {site.checkSsl && (
                            <span className="flex items-center space-x-0.5 text-[10px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-1 py-0.5 rounded">
                              <Lock className="w-2.5 h-2.5" />
                              <span>SSL</span>
                            </span>
                          )}
                        </div>
                        <a
                          href={site.url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs text-blue-600 hover:underline flex items-center space-x-1 mt-0.5 truncate"
                        >
                          <span>{site.url}</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>
                        <p className="text-[11px] text-slate-400 mt-1">
                          Interval: {site.monitoringIntervalSeconds}s • Timeout: {site.requestTimeoutMs}ms • Expected Status: {site.expectedHttpStatusMin}-{site.expectedHttpStatusMax}
                        </p>
                      </div>

                      <div className="text-right flex-shrink-0">
                        <span
                          className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            site.status === 'HEALTHY'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-amber-50 text-amber-700 border border-amber-200'
                          }`}
                        >
                          {site.status}
                        </span>
                        <p className="text-[11px] text-slate-400 mt-1">
                          {site.lastMonitoredAt ? new Date(site.lastMonitoredAt).toLocaleTimeString() : 'Not yet checked'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* SSL Certificate Expiry Alert Card */}
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600" />
                <span>SSL/TLS Certificate Validity</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-emerald-50/50 rounded-lg border border-emerald-100">
                  <p className="font-semibold text-slate-800">Production Wildcard (*.emblemix.io)</p>
                  <p className="text-slate-500 text-[11px] mt-0.5">Let's Encrypt Authority X3</p>
                  <div className="mt-2 flex items-center justify-between text-[11px]">
                    <span className="text-emerald-700 font-bold">68 Days Remaining</span>
                    <span className="text-slate-400">Auto-renews at 30d</span>
                  </div>
                </div>
                <div className="p-3 bg-emerald-50/50 rounded-lg border border-emerald-100">
                  <p className="font-semibold text-slate-800">API Gateway (api.emblemix.io)</p>
                  <p className="text-slate-500 text-[11px] mt-0.5">DigiCert Global Root CA</p>
                  <div className="mt-2 flex items-center justify-between text-[11px]">
                    <span className="text-emerald-700 font-bold">142 Days Remaining</span>
                    <span className="text-slate-400">Valid</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Web Engineering Incidents */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-red-600" />
                <span>Web Incidents</span>
              </CardTitle>
              <Link href="/incidents" className="text-xs text-blue-600 hover:underline">
                View All
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {incLoading ? (
                <div className="p-6 text-center text-slate-400 text-sm">Loading incidents...</div>
              ) : webIncidents.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs flex flex-col items-center">
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 mb-2" />
                  <span>All websites operating normally</span>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {webIncidents.slice(0, 5).map((inc) => (
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
                        Status: {inc.status} • Team: {inc.assignedTeamName || 'Web Engineering'}
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
