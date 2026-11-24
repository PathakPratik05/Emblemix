'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Webhook,
  Plus,
  Play,
  RotateCw,
  Trash2,
  Copy,
  Check,
  History,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  ExternalLink,
} from 'lucide-react';
import { api, WebhookConfiguration, WebhookDeliveryLog } from '../../../lib/api';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { Badge } from '../../../components/ui/Badge';

export default function WebhooksPage() {
  const queryClient = useQueryClient();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [systemId, setSystemId] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Secret display modal state
  const [createdWebhook, setCreatedWebhook] = useState<{ name: string; secret: string; url: string } | null>(null);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const [copiedSecret, setCopiedSecret] = useState(false);

  // Delivery logs state
  const [selectedWebhookForLogs, setSelectedWebhookForLogs] = useState<WebhookConfiguration | null>(null);

  // Test result state
  const [testResult, setTestResult] = useState<{ status: string; message: string; eventId?: string } | null>(null);

  const { data: webhooks, isLoading } = useQuery({
    queryKey: ['webhooks-list'],
    queryFn: () => api.webhooks.getAll(),
  });

  const { data: systemsResponse } = useQuery({
    queryKey: ['connected-systems'],
    queryFn: () => api.systems.getAll(),
  });

  const { data: deliveryLogs, isLoading: logsLoading } = useQuery({
    queryKey: ['webhook-deliveries', selectedWebhookForLogs?.id],
    queryFn: () => (selectedWebhookForLogs ? api.webhooks.getDeliveries(selectedWebhookForLogs.id) : Promise.resolve([])),
    enabled: !!selectedWebhookForLogs,
  });

  const createMutation = useMutation({
    mutationFn: api.webhooks.create,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['webhooks-list'] });
      setIsCreateModalOpen(false);
      resetForm();
      if (data.secret) {
        setCreatedWebhook({ name: data.name, secret: data.secret, url: data.webhookUrl });
      }
    },
    onError: (err: any) => setError(err.message || 'Failed to create webhook'),
  });

  const rotateSecretMutation = useMutation({
    mutationFn: (hook: WebhookConfiguration) => api.webhooks.rotateSecret(hook.id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['webhooks-list'] });
      if (data.secret) {
        setCreatedWebhook({ name: data.name, secret: data.secret, url: data.webhookUrl });
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.webhooks.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks-list'] });
    },
  });

  const testMutation = useMutation({
    mutationFn: (hook: WebhookConfiguration) => api.webhooks.test(hook.id),
    onSuccess: (res) => {
      setTestResult(res);
      queryClient.invalidateQueries({ queryKey: ['webhook-deliveries'] });
      queryClient.invalidateQueries({ queryKey: ['recent-incidents'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-overview'] });
    },
  });

  const resetForm = () => {
    setName('');
    setDescription('');
    setSystemId('');
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      name,
      description: description.trim() || undefined,
      systemId: systemId ? systemId : undefined,
    });
  };

  const copyToClipboard = (text: string, type: 'url' | 'secret') => {
    navigator.clipboard.writeText(text);
    if (type === 'url') {
      setCopiedUrl(text);
      setTimeout(() => setCopiedUrl(null), 2000);
    } else {
      setCopiedSecret(true);
      setTimeout(() => setCopiedSecret(false), 2000);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Custom Webhook Integrations</h2>
          <p className="text-sm text-slate-500 mt-1">
            Inbound HMAC-SHA256 authenticated webhook endpoints for external services, scripts, and monitoring agents.
          </p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>New Webhook Endpoint</span>
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Webhook Name</th>
                  <th className="py-3 px-6">Ingestion URL</th>
                  <th className="py-3 px-6">Linked System</th>
                  <th className="py-3 px-6">Auth Header</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-10 text-slate-400">
                      Loading webhook endpoints...
                    </td>
                  </tr>
                ) : webhooks && webhooks.length > 0 ? (
                  webhooks.map((hook) => (
                    <tr key={hook.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900">
                        <div className="flex items-center space-x-2">
                          <Webhook className="w-4 h-4 text-purple-600 flex-shrink-0" />
                          <span>{hook.name}</span>
                        </div>
                        {hook.description && (
                          <p className="text-xs text-slate-500 font-normal mt-0.5">{hook.description}</p>
                        )}
                      </td>
                      <td className="py-4 px-6 font-mono text-xs text-slate-700 max-w-xs truncate">
                        <div className="flex items-center space-x-1.5">
                          <span className="truncate bg-slate-100 px-2 py-1 rounded border border-slate-200 select-all">
                            {hook.webhookUrl}
                          </span>
                          <button
                            type="button"
                            onClick={() => copyToClipboard(hook.webhookUrl, 'url')}
                            className="p-1 hover:text-blue-600 rounded text-slate-400"
                            title="Copy URL"
                          >
                            {copiedUrl === hook.webhookUrl ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-600 font-medium">
                        {hook.systemName || (
                          <span className="text-slate-400 italic">Default Webhook Service</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-xs">
                        <span className="font-mono bg-purple-50 text-purple-700 border border-purple-200 px-2 py-0.5 rounded">
                          X-Emblemix-Signature
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        {hook.enabled ? (
                          <Badge variant="success">Active</Badge>
                        ) : (
                          <Badge variant="warning">Disabled</Badge>
                        )}
                      </td>
                      <td className="py-4 px-6 text-right space-x-1.5">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => testMutation.mutate(hook)}
                          isLoading={testMutation.isPending && testMutation.variables?.id === hook.id}
                          className="space-x-1"
                          title="Simulate a signed webhook ping"
                        >
                          <Play className="w-3 h-3 text-purple-600" />
                          <span>Test</span>
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedWebhookForLogs(hook)}
                          title="View Ingestion Delivery Logs"
                        >
                          <History className="w-4 h-4 text-slate-500" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            if (window.confirm(`Rotate signing secret for ${hook.name}? Callers must be updated.`)) {
                              rotateSecretMutation.mutate(hook);
                            }
                          }}
                          title="Rotate Secret"
                        >
                          <RotateCw className="w-4 h-4 text-amber-600" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            if (window.confirm(`Delete webhook ${hook.name}?`)) {
                              deleteMutation.mutate(hook.id);
                            }
                          }}
                          className="text-slate-400 hover:text-red-600"
                          title="Delete Webhook"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-slate-400">
                      No webhook endpoints configured yet. Click "New Webhook Endpoint" to create one.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Secret Display & Instructions Modal */}
      <Modal
        isOpen={!!createdWebhook}
        onClose={() => setCreatedWebhook(null)}
        title="Webhook Signing Secret Generated"
        description="Save your secret key securely. It will not be shown again."
        className="max-w-xl"
      >
        {createdWebhook && (
          <div className="space-y-4">
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-xs flex items-start space-x-2">
              <AlertTriangle className="w-5 h-5 flex-shrink-0 text-amber-600 mt-0.5" />
              <p>
                This shared secret is encrypted in the Emblemix database using AES-256-GCM. Copy it now, as
                it will not be displayed again.
              </p>
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-700 block mb-1">Webhook Secret Key</label>
              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  readOnly
                  value={createdWebhook.secret}
                  className="font-mono text-xs bg-slate-50 border border-slate-300 rounded-lg p-2.5 flex-1 select-all"
                />
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => copyToClipboard(createdWebhook.secret, 'secret')}
                  className="space-x-1 flex-shrink-0"
                >
                  {copiedSecret ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedSecret ? 'Copied' : 'Copy'}</span>
                </Button>
              </div>
            </div>

            <div className="bg-slate-900 text-slate-100 p-3.5 rounded-lg text-xs font-mono space-y-2">
              <p className="text-slate-400 font-sans font-semibold">How to sign requests (Bash / cURL):</p>
              <pre className="overflow-x-auto text-[11px] leading-relaxed">
{`# 1. Compute HMAC-SHA256 signature
SIG=$(echo -n '{"status":"FAIL","message":"App Error"}' | \\
  openssl dgst -sha256 -hmac "${createdWebhook.secret}" | cut -d' ' -f2)

# 2. Send webhook POST
curl -X POST "${createdWebhook.url}" \\
  -H "Content-Type: application/json" \\
  -H "X-Emblemix-Signature: sha256=$SIG" \\
  -d '{"status":"FAIL","message":"App Error"}'`}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <Button onClick={() => setCreatedWebhook(null)}>Done</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Simulator Test Result Modal */}
      <Modal
        isOpen={!!testResult}
        onClose={() => setTestResult(null)}
        title="Webhook Test Execution"
        description="Simulated signed payload delivery result"
      >
        {testResult && (
          <div className="space-y-4">
            <div className="p-4 rounded-xl border bg-emerald-50 border-emerald-200 flex items-start space-x-3">
              <CheckCircle2 className="w-6 h-6 text-emerald-600 flex-shrink-0" />
              <div>
                <p className="font-semibold text-slate-900">{testResult.message}</p>
                {testResult.eventId && (
                  <p className="text-xs text-slate-600 font-mono mt-1">Ingested Event ID: {testResult.eventId}</p>
                )}
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={() => setTestResult(null)}>Close</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Delivery Logs Modal */}
      <Modal
        isOpen={!!selectedWebhookForLogs}
        onClose={() => setSelectedWebhookForLogs(null)}
        title={`Delivery History: ${selectedWebhookForLogs?.name}`}
        description="Inbound webhook request attempts and signature verification results"
        className="max-w-2xl"
      >
        <div className="max-h-96 overflow-y-auto space-y-2">
          {logsLoading ? (
            <p className="text-center py-8 text-sm text-slate-400">Loading delivery logs...</p>
          ) : deliveryLogs && deliveryLogs.length > 0 ? (
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b text-slate-500 font-semibold">
                  <th className="py-2">Time</th>
                  <th className="py-2">Status</th>
                  <th className="py-2">HTTP</th>
                  <th className="py-2">Payload Summary</th>
                  <th className="py-2">Error</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                {deliveryLogs.map((log) => (
                  <tr key={log.id}>
                    <td className="py-2 text-slate-600">{new Date(log.receivedAt).toLocaleTimeString()}</td>
                    <td className="py-2">
                      <span
                        className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                          log.status === 'SUCCESS'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {log.status}
                      </span>
                    </td>
                    <td className="py-2 text-slate-900">{log.httpStatusCode}</td>
                    <td className="py-2 text-slate-600 font-sans truncate max-w-xs">{log.payloadSummary || '-'}</td>
                    <td className="py-2 text-red-600 font-sans truncate max-w-xs">{log.errorMessage || 'None'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="text-center py-8 text-sm text-slate-400">No delivery logs recorded yet.</p>
          )}
        </div>
      </Modal>

      {/* Create Webhook Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => {
          setIsCreateModalOpen(false);
          resetForm();
        }}
        title="Create Custom Webhook Endpoint"
        description="Generate a secure inbound endpoint with automated incident routing"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Webhook Name"
            placeholder="e.g. Payment Gateway Failure Alerts"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Associated System (Optional)
            </label>
            <select
              value={systemId}
              onChange={(e) => setSystemId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Default Webhook Ingestion Service</option>
              {systemsResponse?.content?.map((sys) => (
                <option key={sys.id} value={sys.id}>
                  {sys.name} ({sys.environment})
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Description (Optional)"
            placeholder="e.g. Triggers incidents from Stripe webhook failures"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsCreateModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={createMutation.isPending}>
              Generate Webhook Endpoint
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
