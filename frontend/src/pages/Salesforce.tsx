import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Cloud, Plus, Play, Unlink, CheckCircle2, XCircle, AlertTriangle, ExternalLink } from 'lucide-react';
import { api, SalesforceConfig } from '../lib/api';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';

export const Salesforce: React.FC = () => {
  const queryClient = useQueryClient();
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [isSandbox, setIsSandbox] = useState(false);
  const [warnThreshold, setWarnThreshold] = useState(80.0);
  const [critThreshold, setCritThreshold] = useState(95.0);
  const [monitorApex, setMonitorApex] = useState(true);
  const [monitorLogins, setMonitorLogins] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [testResult, setTestResult] = useState<any | null>(null);

  const { data: configs, isLoading } = useQuery({
    queryKey: ['salesforce-configs'],
    queryFn: () => api.salesforce.getAll(),
    refetchInterval: 20000,
  });

  const createMutation = useMutation({
    mutationFn: api.salesforce.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesforce-configs'] });
      setIsAddModalOpen(false);
      resetForm();
    },
    onError: (err: any) => setError(err.message || 'Failed to register Salesforce org'),
  });

  const testMutation = useMutation({
    mutationFn: (config: SalesforceConfig) =>
      api.salesforce.test(config.systemId).then((res) => ({ ...res, name: config.name })),
    onSuccess: (data) => setTestResult(data),
  });

  const disconnectMutation = useMutation({
    mutationFn: (config: SalesforceConfig) => api.salesforce.disconnect(config.systemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesforce-configs'] });
      queryClient.invalidateQueries({ queryKey: ['connected-systems'] });
    },
  });

  const resetForm = () => {
    setName('');
    setIsSandbox(false);
    setWarnThreshold(80.0);
    setCritThreshold(95.0);
    setMonitorApex(true);
    setMonitorLogins(true);
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      name,
      isSandbox,
      apiUsageWarningThresholdPct: Number(warnThreshold),
      apiUsageCriticalThresholdPct: Number(critThreshold),
      monitorApexJobs: monitorApex,
      monitorLoginActivity: monitorLogins,
    });
  };

  const handleConnectOAuth = async (config: SalesforceConfig) => {
    try {
      const res = await api.salesforce.getAuthUrl(config.systemId, config.isSandbox);
      window.location.href = res.authorizationUrl;
    } catch (err: any) {
      alert('Failed to initiate Salesforce OAuth flow: ' + (err.message || err));
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Salesforce Organizations</h2>
          <p className="text-sm text-slate-500 mt-1">
            Real Salesforce OAuth 2.0 Web Server integration, API limit monitoring, and Async Apex Job error detection.
          </p>
        </div>
        <Button onClick={() => setIsAddModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>Add Salesforce Org</span>
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Organization</th>
                  <th className="py-3 px-6">Type</th>
                  <th className="py-3 px-6">Connection</th>
                  <th className="py-3 px-6">API Limit Thresholds</th>
                  <th className="py-3 px-6">Apex Jobs</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-10 text-slate-400">
                      Loading Salesforce organizations...
                    </td>
                  </tr>
                ) : configs && configs.length > 0 ? (
                  configs.map((cfg) => (
                    <tr key={cfg.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900">
                        <div className="flex items-center space-x-2">
                          <Cloud className="w-4 h-4 text-sky-500 flex-shrink-0" />
                          <span>{cfg.name}</span>
                        </div>
                        {cfg.externalOrgId && (
                          <span className="text-[11px] font-mono text-slate-500 block mt-0.5">
                            Org ID: {cfg.externalOrgId}
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-6">
                        <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-700 border">
                          {cfg.isSandbox ? 'Sandbox' : 'Production'}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        {cfg.connected ? (
                          <div className="space-y-1">
                            <Badge variant="success">Connected</Badge>
                            {cfg.instanceUrl && (
                              <p className="text-[11px] font-mono text-slate-500 truncate max-w-[180px]">
                                {cfg.instanceUrl}
                              </p>
                            )}
                          </div>
                        ) : (
                          <Badge variant="warning">Not Connected</Badge>
                        )}
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-600">
                        Warn &gt; {cfg.apiUsageWarningThresholdPct}% | Crit &gt; {cfg.apiUsageCriticalThresholdPct}%
                      </td>
                      <td className="py-4 px-6 text-xs">
                        {cfg.monitorApexJobs ? (
                          <span className="text-emerald-700 font-medium">Monitored</span>
                        ) : (
                          <span className="text-slate-400">Disabled</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-right space-x-2">
                        {cfg.connected ? (
                          <>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => testMutation.mutate(cfg)}
                              isLoading={testMutation.isPending && testMutation.variables?.systemId === cfg.systemId}
                              className="space-x-1"
                            >
                              <Play className="w-3 h-3" />
                              <span>Test Limits</span>
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                if (window.confirm(`Disconnect Salesforce integration for ${cfg.name}?`)) {
                                  disconnectMutation.mutate(cfg);
                                }
                              }}
                              className="text-slate-400 hover:text-red-600"
                              title="Disconnect Integration"
                            >
                              <Unlink className="w-4 h-4" />
                            </Button>
                          </>
                        ) : (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleConnectOAuth(cfg)}
                            className="space-x-1.5 bg-sky-600 hover:bg-sky-700"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                            <span>Connect OAuth</span>
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-slate-400">
                      No Salesforce organizations registered yet. Click "Add Salesforce Org" to connect.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Test Limits Result Modal */}
      <Modal
        isOpen={!!testResult}
        onClose={() => setTestResult(null)}
        title={`Salesforce Live Probe: ${testResult?.name}`}
        description="REST API Limits and Async Apex Job Inspection"
      >
        {testResult && (
          <div className="space-y-4">
            <div
              className={`p-4 rounded-xl border flex items-start space-x-3 ${
                testResult.connected ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200'
              }`}
            >
              {testResult.connected ? (
                <CheckCircle2 className="w-6 h-6 text-emerald-600 flex-shrink-0" />
              ) : (
                <XCircle className="w-6 h-6 text-red-600 flex-shrink-0" />
              )}
              <div>
                <p className="font-semibold text-slate-900">{testResult.message}</p>
                {testResult.error && <p className="text-xs text-red-600 mt-1 font-mono">{testResult.error}</p>}
              </div>
            </div>

            {testResult.limits && (
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
                  Daily REST API Request Limits
                </span>
                <div className="flex justify-between items-baseline">
                  <span className="text-2xl font-black text-slate-900">
                    {testResult.limits.percentageUsed}%
                  </span>
                  <span className="text-xs text-slate-600 font-mono">
                    {testResult.limits.max - testResult.limits.remaining} / {testResult.limits.max} Used
                  </span>
                </div>
                {/* Progress bar */}
                <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${
                      testResult.limits.percentageUsed > 90
                        ? 'bg-red-500'
                        : testResult.limits.percentageUsed > 75
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    }`}
                    style={{ width: `${Math.min(100, testResult.limits.percentageUsed)}%` }}
                  />
                </div>
              </div>
            )}

            <div className="space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
                Recent Failed Async Apex Jobs
              </span>
              {testResult.failedApexJobs && testResult.failedApexJobs.length > 0 ? (
                <div className="max-h-40 overflow-y-auto space-y-1.5 text-xs">
                  {testResult.failedApexJobs.map((job: any) => (
                    <div key={job.id} className="p-2 bg-red-50/60 border border-red-100 rounded text-red-800">
                      <span className="font-semibold font-mono">{job.jobType}</span> - Errors: {job.numberOfErrors}
                      <p className="text-[11px] text-red-700 mt-0.5">{job.extendedStatus || job.status}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-emerald-700 bg-emerald-50 p-2.5 rounded border border-emerald-100">
                  No failed or aborted Apex jobs detected.
                </p>
              )}
            </div>

            <div className="flex justify-end pt-3">
              <Button onClick={() => setTestResult(null)}>Close</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Add Salesforce Org Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => {
          setIsAddModalOpen(false);
          resetForm();
        }}
        title="Register Salesforce Organization"
        description="Configure monitoring parameters for your Salesforce instance"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="System / Organization Name"
            placeholder="e.g. Acme Enterprise CRM"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <div className="flex items-center space-x-2 pt-1">
            <input
              id="isSandbox"
              type="checkbox"
              checked={isSandbox}
              onChange={(e) => setIsSandbox(e.target.checked)}
              className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
            />
            <label htmlFor="isSandbox" className="text-sm font-medium text-slate-700">
              This is a Salesforce Sandbox Org (uses test.salesforce.com)
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="API Warning Threshold (%)"
              type="number"
              min={1}
              max={100}
              value={warnThreshold}
              onChange={(e) => setWarnThreshold(Number(e.target.value))}
              required
            />
            <Input
              label="API Critical Threshold (%)"
              type="number"
              min={1}
              max={100}
              value={critThreshold}
              onChange={(e) => setCritThreshold(Number(e.target.value))}
              required
            />
          </div>

          <div className="space-y-2 pt-2 border-t border-slate-100">
            <div className="flex items-center space-x-2">
              <input
                id="monitorApex"
                type="checkbox"
                checked={monitorApex}
                onChange={(e) => setMonitorApex(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="monitorApex" className="text-sm text-slate-700">
                Monitor Async Apex Job Failures & Abortions
              </label>
            </div>
            <div className="flex items-center space-x-2">
              <input
                id="monitorLogins"
                type="checkbox"
                checked={monitorLogins}
                onChange={(e) => setMonitorLogins(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="monitorLogins" className="text-sm text-slate-700">
                Monitor Repeated Login Failures
              </label>
            </div>
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
              Register Salesforce Org
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
