import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Globe, Plus, Play, History, Shield, CheckCircle2, XCircle, AlertTriangle, ExternalLink } from 'lucide-react';
import { api, WebsiteConfiguration, MonitoringResult } from '../lib/api';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';

export const Websites: React.FC = () => {
  const queryClient = useQueryClient();
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [testResult, setTestResult] = useState<{
    siteName: string;
    healthy: boolean;
    httpStatusCode?: number;
    responseTimeMs?: number;
    sslValid?: boolean;
    sslDaysRemaining?: number;
    message: string;
    error?: string;
  } | null>(null);
  const [historySite, setHistorySite] = useState<WebsiteConfiguration | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [interval, setInterval] = useState(60);
  const [timeoutMs, setTimeoutMs] = useState(5000);
  const [statusMin, setStatusMin] = useState(200);
  const [statusMax, setStatusMax] = useState(299);
  const [checkSsl, setCheckSsl] = useState(true);
  const [expectedContent, setExpectedContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data: websitesResponse, isLoading } = useQuery({
    queryKey: ['monitored-websites'],
    queryFn: () => api.websites.getAll(),
    refetchInterval: 15000,
  });

  const { data: historyResponse, isLoading: historyLoading } = useQuery({
    queryKey: ['website-history', historySite?.systemId],
    queryFn: () => (historySite ? api.websites.getHistory(historySite.systemId) : Promise.resolve(null)),
    enabled: !!historySite,
  });

  const createMutation = useMutation({
    mutationFn: api.websites.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monitored-websites'] });
      queryClient.invalidateQueries({ queryKey: ['connected-systems'] });
      setIsAddModalOpen(false);
      resetForm();
    },
    onError: (err: any) => setError(err.message || 'Failed to register website'),
  });

  const testMutation = useMutation({
    mutationFn: (site: WebsiteConfiguration) =>
      api.websites.test(site.id).then((res) => ({ ...res, siteName: site.name })),
    onSuccess: (data) => setTestResult(data),
  });

  const resetForm = () => {
    setName('');
    setUrl('');
    setInterval(60);
    setTimeoutMs(5000);
    setStatusMin(200);
    setStatusMax(299);
    setCheckSsl(true);
    setExpectedContent('');
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    createMutation.mutate({
      name,
      url,
      monitoringIntervalSeconds: Number(interval),
      requestTimeoutMs: Number(timeoutMs),
      expectedHttpStatusMin: Number(statusMin),
      expectedHttpStatusMax: Number(statusMax),
      expectedResponseContent: expectedContent.trim() ? expectedContent.trim() : undefined,
      checkSsl,
    });
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Website Monitoring</h2>
          <p className="text-sm text-slate-500 mt-1">
            Automated HTTP availability, latency, SSL expiration, and SSRF-protected health checks.
          </p>
        </div>
        <Button onClick={() => setIsAddModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>Add Website</span>
        </Button>
      </div>

      {/* Websites Listing */}
      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Website</th>
                  <th className="py-3 px-6">Target URL</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6">Interval</th>
                  <th className="py-3 px-6">SSL Check</th>
                  <th className="py-3 px-6">Last Check</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-10 text-slate-400">
                      Loading monitored websites...
                    </td>
                  </tr>
                ) : websitesResponse?.content && websitesResponse.content.length > 0 ? (
                  websitesResponse.content.map((site) => (
                    <tr key={site.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900">
                        <div className="flex items-center space-x-2">
                          <Globe className="w-4 h-4 text-blue-600 flex-shrink-0" />
                          <span>{site.name}</span>
                        </div>
                      </td>
                      <td className="py-4 px-6 font-mono text-xs text-blue-600 truncate max-w-xs">
                        <a
                          href={site.url}
                          target="_blank"
                          rel="noreferrer"
                          className="hover:underline flex items-center space-x-1"
                        >
                          <span className="truncate">{site.url}</span>
                          <ExternalLink className="w-3 h-3 flex-shrink-0 inline" />
                        </a>
                      </td>
                      <td className="py-4 px-6">
                        <Badge
                          variant={
                            site.status === 'HEALTHY'
                              ? 'success'
                              : site.status === 'DOWN'
                              ? 'destructive'
                              : site.status === 'DEGRADED'
                              ? 'warning'
                              : 'default'
                          }
                        >
                          {site.status}
                        </Badge>
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-600">{site.monitoringIntervalSeconds}s</td>
                      <td className="py-4 px-6 text-xs">
                        {site.checkSsl ? (
                          <span className="text-emerald-700 font-medium flex items-center">
                            <Shield className="w-3.5 h-3.5 mr-1" /> Enabled ({site.sslExpiryThresholdDays}d)
                          </span>
                        ) : (
                          <span className="text-slate-400">Disabled</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-500">
                        {site.lastMonitoredAt ? new Date(site.lastMonitoredAt).toLocaleTimeString() : 'Pending'}
                      </td>
                      <td className="py-4 px-6 text-right space-x-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => testMutation.mutate(site)}
                          isLoading={testMutation.isPending && testMutation.variables?.id === site.id}
                          className="space-x-1"
                          title="Execute live test check"
                        >
                          <Play className="w-3 h-3" />
                          <span>Test</span>
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setHistorySite(site)}
                          title="View Latency History"
                        >
                          <History className="w-4 h-4 text-slate-500" />
                        </Button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="text-center py-12 text-slate-400">
                      No websites registered yet. Click "Add Website" to begin automated checks.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Live Test Result Modal */}
      <Modal
        isOpen={!!testResult}
        onClose={() => setTestResult(null)}
        title={`Live Check Result: ${testResult?.siteName}`}
        description="Immediate on-demand diagnostic probe"
      >
        {testResult && (
          <div className="space-y-4">
            <div
              className={`p-4 rounded-xl border flex items-start space-x-3 ${
                testResult.healthy ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'
              }`}
            >
              {testResult.healthy ? (
                <CheckCircle2 className="w-6 h-6 text-emerald-600 flex-shrink-0" />
              ) : (
                <XCircle className="w-6 h-6 text-red-600 flex-shrink-0" />
              )}
              <div>
                <p className="font-semibold text-slate-900">{testResult.message}</p>
                {testResult.error && <p className="text-xs text-red-600 mt-1 font-mono">{testResult.error}</p>}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-xs text-slate-500 block">HTTP Response Code</span>
                <span className="text-lg font-bold text-slate-900 font-mono">
                  {testResult.httpStatusCode ?? 'N/A'}
                </span>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-xs text-slate-500 block">Response Time</span>
                <span className="text-lg font-bold text-slate-900 font-mono">
                  {testResult.responseTimeMs ? `${testResult.responseTimeMs} ms` : 'N/A'}
                </span>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-xs text-slate-500 block">SSL Certificate</span>
                <span className="text-sm font-semibold text-slate-800">
                  {testResult.sslValid ? 'Valid' : testResult.sslValid === false ? 'Invalid' : 'Not Tested'}
                </span>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-xs text-slate-500 block">SSL Days Remaining</span>
                <span className="text-sm font-semibold text-slate-800">
                  {testResult.sslDaysRemaining != null ? `${testResult.sslDaysRemaining} days` : 'N/A'}
                </span>
              </div>
            </div>

            <div className="flex justify-end pt-3">
              <Button onClick={() => setTestResult(null)}>Close</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Latency History Modal */}
      <Modal
        isOpen={!!historySite}
        onClose={() => setHistorySite(null)}
        title={`Check History: ${historySite?.name}`}
        description="Historical probe results and response latency"
        className="max-w-2xl"
      >
        <div className="max-h-96 overflow-y-auto space-y-2">
          {historyLoading ? (
            <p className="text-center py-8 text-sm text-slate-400">Loading history logs...</p>
          ) : historyResponse?.content && historyResponse.content.length > 0 ? (
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b text-slate-500 font-semibold">
                  <th className="py-2">Time</th>
                  <th className="py-2">Status</th>
                  <th className="py-2">Code</th>
                  <th className="py-2">Latency</th>
                  <th className="py-2">Error Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                {historyResponse.content.map((res) => (
                  <tr key={res.id}>
                    <td className="py-2 text-slate-600">{new Date(res.checkedAt).toLocaleTimeString()}</td>
                    <td className="py-2">
                      <span
                        className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                          res.status === 'SUCCESS' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {res.status}
                      </span>
                    </td>
                    <td className="py-2 text-slate-900">{res.httpStatusCode ?? '-'}</td>
                    <td className="py-2 text-slate-900">{res.responseTimeMs ? `${res.responseTimeMs}ms` : '-'}</td>
                    <td className="py-2 text-red-600 font-sans truncate max-w-xs">{res.errorMessage || 'None'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="text-center py-8 text-sm text-slate-400">No historical checks logged yet.</p>
          )}
        </div>
      </Modal>

      {/* Add Website Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => {
          setIsAddModalOpen(false);
          resetForm();
        }}
        title="Register Website for Monitoring"
        description="Provide endpoint details and alerting thresholds"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Website Name"
            placeholder="e.g. Acme Production Storefront"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Target URL (HTTP / HTTPS)"
            placeholder="https://example.com"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            helperText="Loopback, metadata, and private IP addresses are automatically blocked for SSRF security."
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Interval (Seconds)"
              type="number"
              min={10}
              value={interval}
              onChange={(e) => setInterval(Number(e.target.value))}
              required
            />
            <Input
              label="Timeout (ms)"
              type="number"
              min={1000}
              max={30000}
              value={timeoutMs}
              onChange={(e) => setTimeoutMs(Number(e.target.value))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Expected Min Status"
              type="number"
              min={100}
              max={599}
              value={statusMin}
              onChange={(e) => setStatusMin(Number(e.target.value))}
              required
            />
            <Input
              label="Expected Max Status"
              type="number"
              min={100}
              max={599}
              value={statusMax}
              onChange={(e) => setStatusMax(Number(e.target.value))}
              required
            />
          </div>

          <Input
            label="Expected Response Content (Optional Substring)"
            placeholder="e.g. Acme Store OK"
            value={expectedContent}
            onChange={(e) => setExpectedContent(e.target.value)}
          />

          <div className="flex items-center space-x-2 pt-1">
            <input
              id="checkSsl"
              type="checkbox"
              checked={checkSsl}
              onChange={(e) => setCheckSsl(e.target.checked)}
              className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
            />
            <label htmlFor="checkSsl" className="text-sm font-medium text-slate-700">
              Verify SSL Certificate & Expiry (Warn if &lt;30 days remaining)
            </label>
          </div>

          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsAddModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={createMutation.isPending}>
              Register Website
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
