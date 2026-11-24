'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Sliders, Plus, Trash2 } from 'lucide-react';
import { api, AlertRule } from '../../../lib/api';
import { Card, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { Badge } from '../../../components/ui/Badge';

export default function AlertRulesPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [name, setName] = useState('');
  const [systemId, setSystemId] = useState('');
  const [eventType, setEventType] = useState('WEBSITE_DOWN');
  const [targetSeverity, setTargetSeverity] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('CRITICAL');
  const [failureThreshold, setFailureThreshold] = useState(2);
  const [latencyThreshold, setLatencyThreshold] = useState<number | ''>('');
  const [apiUsageThreshold, setApiUsageThreshold] = useState<number | ''>('');
  const [error, setError] = useState<string | null>(null);

  const { data: rules, isLoading } = useQuery({
    queryKey: ['alert-rules'],
    queryFn: api.alertRules.getAll,
  });

  const { data: systemsResponse } = useQuery({
    queryKey: ['connected-systems'],
    queryFn: () => api.systems.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: api.alertRules.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alert-rules'] });
      setIsModalOpen(false);
      resetForm();
    },
    onError: (err: any) => setError(err.message || 'Failed to create alert rule'),
  });

  const deleteMutation = useMutation({
    mutationFn: api.alertRules.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['alert-rules'] }),
  });

  const resetForm = () => {
    setName('');
    setSystemId('');
    setEventType('WEBSITE_DOWN');
    setTargetSeverity('CRITICAL');
    setFailureThreshold(2);
    setLatencyThreshold('');
    setApiUsageThreshold('');
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      name,
      systemId: systemId ? systemId : undefined,
      eventType,
      targetSeverity,
      failureCountThreshold: Number(failureThreshold),
      responseTimeThresholdMs: latencyThreshold !== '' ? Number(latencyThreshold) : undefined,
      apiUsagePctThreshold: apiUsageThreshold !== '' ? Number(apiUsageThreshold) : undefined,
      enabled: true,
    });
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Alert Rule Engine</h2>
          <p className="text-sm text-slate-500 mt-1">
            Configure automated thresholds, failure counts, and severity assignments for monitoring events.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>New Alert Rule</span>
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Rule Name</th>
                  <th className="py-3 px-6">Target System</th>
                  <th className="py-3 px-6">Event Type</th>
                  <th className="py-3 px-6">Target Severity</th>
                  <th className="py-3 px-6">Thresholds</th>
                  <th className="py-3 px-6">State</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-10 text-slate-400">
                      Loading alert rules...
                    </td>
                  </tr>
                ) : rules && rules.length > 0 ? (
                  rules.map((rule) => (
                    <tr key={rule.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900">{rule.name}</td>
                      <td className="py-4 px-6 text-slate-700 font-medium">
                        {rule.systemName || <span className="text-slate-400 italic">All Systems</span>}
                      </td>
                      <td className="py-4 px-6 font-mono text-xs text-blue-600 font-semibold">
                        {rule.eventType}
                      </td>
                      <td className="py-4 px-6">
                        <Badge
                          variant={
                            rule.targetSeverity === 'CRITICAL'
                              ? 'destructive'
                              : rule.targetSeverity === 'HIGH'
                              ? 'warning'
                              : rule.targetSeverity === 'MEDIUM'
                              ? 'info'
                              : 'default'
                          }
                        >
                          {rule.targetSeverity}
                        </Badge>
                      </td>
                      <td className="py-4 px-6 text-xs text-slate-600 space-y-0.5">
                        <p>Failures &ge; {rule.failureCountThreshold}</p>
                        {rule.responseTimeThresholdMs && <p>Latency &gt; {rule.responseTimeThresholdMs}ms</p>}
                        {rule.apiUsagePctThreshold && <p>API Usage &gt; {rule.apiUsagePctThreshold}%</p>}
                      </td>
                      <td className="py-4 px-6">
                        <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">
                          Active
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            if (window.confirm(`Delete alert rule "${rule.name}"?`)) {
                              deleteMutation.mutate(rule.id);
                            }
                          }}
                          className="text-slate-400 hover:text-red-600"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="text-center py-12 text-slate-400">
                      No alert rules defined. Create a rule to automatically trigger incidents.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Create Alert Rule Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          resetForm();
        }}
        title="Create Configurable Alert Rule"
        description="Define conditions that elevate normalized events into actionable incidents"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Rule Name"
            placeholder="e.g. Critical Website Outage"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Target System</label>
            <select
              value={systemId}
              onChange={(e) => setSystemId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Apply to All Connected Systems</option>
              {systemsResponse?.content?.map((sys) => (
                <option key={sys.id} value={sys.id}>
                  {sys.name} ({sys.systemType})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Event Type</label>
              <select
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="WEBSITE_DOWN">WEBSITE_DOWN</option>
                <option value="HTTP_ERROR">HTTP_ERROR</option>
                <option value="HIGH_RESPONSE_TIME">HIGH_RESPONSE_TIME</option>
                <option value="SSL_EXPIRY_WARNING">SSL_EXPIRY_WARNING</option>
                <option value="SALESFORCE_API_LIMIT_CRITICAL">SALESFORCE_API_LIMIT_CRITICAL</option>
                <option value="SALESFORCE_API_LIMIT_WARNING">SALESFORCE_API_LIMIT_WARNING</option>
                <option value="APEX_JOB_FAILED">APEX_JOB_FAILED</option>
                <option value="SALESFORCE_LOGIN_FAILURE">SALESFORCE_LOGIN_FAILURE</option>
                <option value="WEBHOOK_INCIDENT">WEBHOOK_INCIDENT</option>
                <option value="CUSTOM">CUSTOM</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Target Severity</label>
              <select
                value={targetSeverity}
                onChange={(e) => setTargetSeverity(e.target.value as any)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="CRITICAL">CRITICAL</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
          </div>

          <Input
            label="Consecutive Failures Threshold"
            type="number"
            min={1}
            max={20}
            value={failureThreshold}
            onChange={(e) => setFailureThreshold(Number(e.target.value))}
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Latency Threshold (ms, Optional)"
              type="number"
              placeholder="e.g. 2000"
              value={latencyThreshold}
              onChange={(e) => setLatencyThreshold(e.target.value ? Number(e.target.value) : '')}
            />
            <Input
              label="API Usage Threshold (%, Optional)"
              type="number"
              placeholder="e.g. 90.0"
              value={apiUsageThreshold}
              onChange={(e) => setApiUsageThreshold(e.target.value ? Number(e.target.value) : '')}
            />
          </div>

          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={createMutation.isPending}>
              Create Alert Rule
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
