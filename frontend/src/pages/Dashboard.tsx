import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Server,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Activity,
  Globe,
  Cloud,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  BarChart,
  Bar,
  Cell,
} from 'recharts';
import { api } from '../lib/api';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';

export const Dashboard: React.FC = () => {
  const { data: overview, isLoading: overviewLoading } = useQuery({
    queryKey: ['dashboard-overview'],
    queryFn: api.dashboard.getOverview,
    refetchInterval: 15000,
  });

  const { data: trends } = useQuery({
    queryKey: ['incident-trends'],
    queryFn: () => api.dashboard.getIncidentTrends(14),
    refetchInterval: 30000,
  });

  const { data: recentIncidents } = useQuery({
    queryKey: ['recent-incidents'],
    queryFn: () => api.incidents.getAll({ page: 0, size: 5 }),
    refetchInterval: 15000,
  });

  const severityColors: Record<string, string> = {
    CRITICAL: '#ef4444',
    HIGH: '#f97316',
    MEDIUM: '#facc15',
    LOW: '#3b82f6',
  };

  const severityBarData = overview?.incidentSeverityDistribution
    ? Object.entries(overview.incidentSeverityDistribution).map(([name, count]) => ({
        name,
        count,
        color: severityColors[name] || '#94a3b8',
      }))
    : [];

  const healthData = overview
    ? [
        { name: 'Healthy', count: overview.healthySystems, color: '#10b981' },
        { name: 'Degraded', count: overview.degradedSystems, color: '#f59e0b' },
        { name: 'Down', count: overview.downSystems, color: '#ef4444' },
      ]
    : [];

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Top Banner & Quick Actions */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Platform Overview</h2>
          <p className="text-sm text-slate-500 mt-1">
            Real-time status of multi-tenant infrastructure, websites, and Salesforce orgs.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Link to="/websites">
            <Button variant="outline" size="sm" className="space-x-1.5">
              <Globe className="w-4 h-4 text-blue-600" />
              <span>Add Website</span>
            </Button>
          </Link>
          <Link to="/salesforce">
            <Button variant="outline" size="sm" className="space-x-1.5">
              <Cloud className="w-4 h-4 text-sky-500" />
              <span>Connect Salesforce</span>
            </Button>
          </Link>
          <Link to="/incidents">
            <Button size="sm" className="space-x-1.5">
              <AlertTriangle className="w-4 h-4" />
              <span>View Incidents</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* Widget Counter Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="hover:shadow-md transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">Connected Systems</p>
                <p className="text-3xl font-extrabold text-slate-900 mt-2">
                  {overviewLoading ? '...' : overview?.totalSystems ?? 0}
                </p>
              </div>
              <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center">
                <Server className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center space-x-2 text-xs font-medium">
              <span className="text-emerald-600 flex items-center">
                <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                {overview?.healthySystems ?? 0} Healthy
              </span>
              <span className="text-slate-300">•</span>
              <span className="text-amber-600">{overview?.degradedSystems ?? 0} Degraded</span>
              <span className="text-slate-300">•</span>
              <span className="text-red-600">{overview?.downSystems ?? 0} Down</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">Open Incidents</p>
                <p className="text-3xl font-extrabold text-slate-900 mt-2">
                  {overviewLoading ? '...' : overview?.openIncidents ?? 0}
                </p>
              </div>
              <div className="w-12 h-12 bg-red-50 text-red-600 rounded-xl flex items-center justify-center">
                <AlertTriangle className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-xs">
              <span className="text-red-600 font-semibold mr-1.5">
                {overview?.criticalIncidents ?? 0} Critical
              </span>
              <span className="text-slate-500">requiring immediate attention</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">Resolved Today</p>
                <p className="text-3xl font-extrabold text-emerald-600 mt-2">
                  {overviewLoading ? '...' : overview?.resolvedTodayIncidents ?? 0}
                </p>
              </div>
              <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-xl flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-xs text-slate-500">
              <Clock className="w-3.5 h-3.5 mr-1 text-slate-400" />
              <span>Avg MTTR: {overview?.averageResolutionTimeMinutes ?? 0} mins</span>
            </div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">Website Uptime (24h)</p>
                <p className="text-3xl font-extrabold text-slate-900 mt-2">
                  {overviewLoading ? '...' : `${overview?.websiteUptimePercentage ?? 100}%`}
                </p>
              </div>
              <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center">
                <Activity className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-xs text-slate-500">
              <TrendingUp className="w-3.5 h-3.5 mr-1 text-indigo-500" />
              <span>Avg Latency: {overview?.averageResponseTimeMs ?? 0} ms</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Visual Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Incident Trend Line Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Incident Activity Trends (Last 14 Days)</CardTitle>
            <CardDescription>Daily comparison of newly detected vs resolved incidents</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-72 w-full">
              {trends && trends.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trends} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorInc" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#ef4444" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="colorRes" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#64748b' }} />
                    <YAxis tick={{ fontSize: 11, fill: '#64748b' }} allowDecimals={false} />
                    <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #cbd5e1' }} />
                    <Area
                      type="monotone"
                      dataKey="count"
                      name="New Incidents"
                      stroke="#ef4444"
                      strokeWidth={2}
                      fillOpacity={1}
                      fill="url(#colorInc)"
                    />
                    <Area
                      type="monotone"
                      dataKey="resolved"
                      name="Resolved"
                      stroke="#10b981"
                      strokeWidth={2}
                      fillOpacity={1}
                      fill="url(#colorRes)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-slate-400 text-sm">
                  No incident trend data recorded yet
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* System Health Breakdown Bar Chart */}
        <Card>
          <CardHeader>
            <CardTitle>System Health Distribution</CardTitle>
            <CardDescription>Current operational state of monitored systems</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={healthData} margin={{ top: 20, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#64748b' }} />
                  <YAxis tick={{ fontSize: 12, fill: '#64748b' }} allowDecimals={false} />
                  <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #cbd5e1' }} />
                  <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                    {healthData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Incidents Table */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Recent Incidents</CardTitle>
            <CardDescription>Latest alerts and triggered failures across your systems</CardDescription>
          </div>
          <Link to="/incidents" className="text-sm font-semibold text-blue-600 hover:text-blue-700 flex items-center">
            <span>View All</span>
            <ArrowRight className="w-4 h-4 ml-1" />
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/75 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Severity</th>
                  <th className="py-3 px-6">System</th>
                  <th className="py-3 px-6">Title</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6">Created</th>
                  <th className="py-3 px-6 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentIncidents?.content && recentIncidents.content.length > 0 ? (
                  recentIncidents.content.map((inc) => (
                    <tr key={inc.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3.5 px-6">
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
                      <td className="py-3.5 px-6 font-medium text-slate-900">{inc.systemName}</td>
                      <td className="py-3.5 px-6 max-w-xs truncate text-slate-700">{inc.title}</td>
                      <td className="py-3.5 px-6">
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
                      <td className="py-3.5 px-6 text-slate-500 text-xs">
                        {new Date(inc.createdAt).toLocaleString()}
                      </td>
                      <td className="py-3.5 px-6 text-right">
                        <Link to={`/incidents/${inc.id}`}>
                          <Button variant="ghost" size="sm">
                            Inspect
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-slate-400">
                      No incidents logged. All monitored systems are operating smoothly!
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
};
