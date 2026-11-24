'use client';

import React from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  Globe,
  Cloud,
  Webhook,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Layers,
  Cpu,
  Activity,
} from 'lucide-react';
import { api } from '../../../lib/api';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../../components/ui/Card';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';

export default function IntegrationsHubPage() {
  const { data: websites } = useQuery({
    queryKey: ['website-configs'],
    queryFn: () => api.websites.getAll(),
  });

  const { data: sfConfigs } = useQuery({
    queryKey: ['salesforce-configs'],
    queryFn: () => api.salesforce.getAll(),
  });

  const { data: webhooks } = useQuery({
    queryKey: ['webhooks-list'],
    queryFn: () => api.webhooks.getAll(),
  });

  const activeWebsites = websites?.content ? websites.content.filter((w) => w.enabled).length : 0;
  const connectedSalesforce = sfConfigs ? sfConfigs.filter((s) => s.connected).length : 0;
  const activeWebhooks = webhooks ? webhooks.filter((w) => w.enabled).length : 0;

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Integrations Hub</h2>
        <p className="text-sm text-slate-500 mt-1">
          Universal connectors for websites, SaaS clouds, and inbound event webhooks.
        </p>
      </div>

      {/* Primary Production Integrations */}
      <div>
        <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 mb-4">
          Active Integration Types
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Website Monitoring */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center mb-2">
                <Globe className="w-5 h-5" />
              </div>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Website Monitoring</CardTitle>
                <Badge variant="success">Production Ready</Badge>
              </div>
              <CardDescription>
                Periodic HTTP/HTTPS health checks, latency tracking, SSL expiry alerts, and SSRF loopback safety.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                <span>Active Targets:</span>
                <span className="font-bold text-slate-900">{activeWebsites} configured</span>
              </div>
              <Link href="/websites" className="block">
                <Button variant="outline" className="w-full justify-between">
                  <span>Manage Websites</span>
                  <ArrowRight className="w-4 h-4 ml-1" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Salesforce Integration */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-sky-50 text-sky-500 flex items-center justify-center mb-2">
                <Cloud className="w-5 h-5" />
              </div>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Salesforce Monitoring</CardTitle>
                <Badge variant="success">Deep Integration</Badge>
              </div>
              <CardDescription>
                Production & Sandbox OAuth2, REST API limits polling, Apex job failure triggers, and encrypted tokens.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                <span>Connected Orgs:</span>
                <span className="font-bold text-slate-900">{connectedSalesforce} connected</span>
              </div>
              <Link href="/salesforce" className="block">
                <Button variant="outline" className="w-full justify-between">
                  <span>Manage Salesforce</span>
                  <ArrowRight className="w-4 h-4 ml-1" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Custom Webhooks */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center mb-2">
                <Webhook className="w-5 h-5" />
              </div>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Custom Webhooks</CardTitle>
                <Badge variant="default" className="bg-purple-100 text-purple-800">
                  New Integration
                </Badge>
              </div>
              <CardDescription>
                Inbound HMAC-SHA256 authenticated webhook endpoints, secret rotation, delivery logs, and alert routing.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                <span>Configured Webhooks:</span>
                <span className="font-bold text-slate-900">{activeWebhooks} active</span>
              </div>
              <Link href="/webhooks" className="block">
                <Button variant="outline" className="w-full justify-between">
                  <span>Manage Webhooks</span>
                  <ArrowRight className="w-4 h-4 ml-1" />
                </Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Future Integrations Roadmap */}
      <div>
        <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 mb-4">
          Future Integration Roadmap (Pluggable Architecture)
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="bg-slate-50/60 border-dashed border-slate-300">
            <CardContent className="p-5 space-y-2">
              <div className="flex items-center space-x-2 text-slate-700 font-semibold text-sm">
                <Layers className="w-4 h-4 text-orange-500" />
                <span>AWS CloudWatch</span>
              </div>
              <p className="text-xs text-slate-500">
                Alarm streams, Lambda errors, and RDS performance threshold monitoring.
              </p>
              <Badge variant="default" className="text-[10px] bg-slate-200 text-slate-600">
                Coming Soon
              </Badge>
            </CardContent>
          </Card>

          <Card className="bg-slate-50/60 border-dashed border-slate-300">
            <CardContent className="p-5 space-y-2">
              <div className="flex items-center space-x-2 text-slate-700 font-semibold text-sm">
                <Cpu className="w-4 h-4 text-blue-500" />
                <span>Azure Monitor</span>
              </div>
              <p className="text-xs text-slate-500">
                Application Insights telemetry, metric alerts, and resource health.
              </p>
              <Badge variant="default" className="text-[10px] bg-slate-200 text-slate-600">
                Coming Soon
              </Badge>
            </CardContent>
          </Card>

          <Card className="bg-slate-50/60 border-dashed border-slate-300">
            <CardContent className="p-5 space-y-2">
              <div className="flex items-center space-x-2 text-slate-700 font-semibold text-sm">
                <Activity className="w-4 h-4 text-indigo-500" />
                <span>Kubernetes</span>
              </div>
              <p className="text-xs text-slate-500">
                Pod crash loops, deployment status, and node pressure telemetry.
              </p>
              <Badge variant="default" className="text-[10px] bg-slate-200 text-slate-600">
                Coming Soon
              </Badge>
            </CardContent>
          </Card>

          <Card className="bg-slate-50/60 border-dashed border-slate-300">
            <CardContent className="p-5 space-y-2">
              <div className="flex items-center space-x-2 text-slate-700 font-semibold text-sm">
                <ShieldCheck className="w-4 h-4 text-rose-500" />
                <span>Prometheus</span>
              </div>
              <p className="text-xs text-slate-500">
                PromQL alert manager endpoints and multi-cluster metric scrapers.
              </p>
              <Badge variant="default" className="text-[10px] bg-slate-200 text-slate-600">
                Coming Soon
              </Badge>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
