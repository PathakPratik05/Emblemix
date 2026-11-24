'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Server, Plus, Trash2, Globe, Cloud, Webhook, CheckCircle, AlertTriangle, XCircle, HelpCircle } from 'lucide-react';
import { api, ConnectedSystem } from '../../../lib/api';
import { Card, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { Badge } from '../../../components/ui/Badge';

export default function SystemsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [systemType, setSystemType] = useState<'WEBSITE' | 'SALESFORCE' | 'WEBHOOK'>('WEBSITE');
  const [description, setDescription] = useState('');
  const [environment, setEnvironment] = useState('PRODUCTION');
  const [error, setError] = useState<string | null>(null);

  const { data: systemsResponse, isLoading } = useQuery({
    queryKey: ['connected-systems'],
    queryFn: () => api.systems.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: api.systems.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connected-systems'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-overview'] });
      setIsModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setError(err.message || 'Failed to register system');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: api.systems.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connected-systems'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-overview'] });
    },
  });

  const resetForm = () => {
    setName('');
    setDescription('');
    setSystemType('WEBSITE');
    setEnvironment('PRODUCTION');
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    createMutation.mutate({ name, systemType, description, environment });
  };

  const getStatusBadge = (status: ConnectedSystem['status']) => {
    switch (status) {
      case 'HEALTHY':
        return (
          <Badge variant="success" className="flex items-center space-x-1">
            <CheckCircle className="w-3 h-3 mr-1" /> Healthy
          </Badge>
        );
      case 'DEGRADED':
        return (
          <Badge variant="warning" className="flex items-center space-x-1">
            <AlertTriangle className="w-3 h-3 mr-1" /> Degraded
          </Badge>
        );
      case 'DOWN':
        return (
          <Badge variant="destructive" className="flex items-center space-x-1">
            <XCircle className="w-3 h-3 mr-1" /> Down
          </Badge>
        );
      default:
        return (
          <Badge variant="default" className="flex items-center space-x-1">
            <HelpCircle className="w-3 h-3 mr-1" /> Unknown
          </Badge>
        );
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Connected Systems</h2>
          <p className="text-sm text-slate-500 mt-1">
            Manage your monitored websites, cloud platforms, webhooks, and services.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>Register System</span>
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">System Name</th>
                  <th className="py-3 px-6">Type</th>
                  <th className="py-3 px-6">Environment</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6">Last Monitored</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-10 text-slate-400">
                      Loading connected systems...
                    </td>
                  </tr>
                ) : systemsResponse?.content && systemsResponse.content.length > 0 ? (
                  systemsResponse.content.map((sys) => (
                    <tr key={sys.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-medium text-slate-900">
                        <div>
                          <span>{sys.name}</span>
                          {sys.description && (
                            <p className="text-xs text-slate-500 font-normal mt-0.5">{sys.description}</p>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex items-center space-x-1.5 text-xs font-semibold text-slate-700">
                          {sys.systemType === 'WEBSITE' ? (
                            <>
                              <Globe className="w-4 h-4 text-blue-600" />
                              <span>Website</span>
                            </>
                          ) : sys.systemType === 'SALESFORCE' ? (
                            <>
                              <Cloud className="w-4 h-4 text-sky-500" />
                              <span>Salesforce</span>
                            </>
                          ) : (
                            <>
                              <Webhook className="w-4 h-4 text-purple-600" />
                              <span>Webhook</span>
                            </>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                          {sys.environment}
                        </span>
                      </td>
                      <td className="py-4 px-6">{getStatusBadge(sys.status)}</td>
                      <td className="py-4 px-6 text-xs text-slate-500">
                        {sys.lastMonitoredAt ? new Date(sys.lastMonitoredAt).toLocaleString() : 'Not yet checked'}
                      </td>
                      <td className="py-4 px-6 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            if (window.confirm(`Are you sure you want to delete ${sys.name}?`)) {
                              deleteMutation.mutate(sys.id);
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
                    <td colSpan={6} className="text-center py-12 text-slate-400">
                      No connected systems registered yet. Click "Register System" to get started.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Register System Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          resetForm();
        }}
        title="Register Connected System"
        description="Add a new target system to track health and trigger alerts"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="System Name"
            placeholder="e.g. Production API Gateway"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">System Type</label>
            <select
              value={systemType}
              onChange={(e) => setSystemType(e.target.value as any)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="WEBSITE">Website (HTTP / HTTPS)</option>
              <option value="SALESFORCE">Salesforce Organization</option>
              <option value="WEBHOOK">Custom Webhook Inbound</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Environment</label>
            <select
              value={environment}
              onChange={(e) => setEnvironment(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="PRODUCTION">Production</option>
              <option value="STAGING">Staging</option>
              <option value="DEVELOPMENT">Development</option>
            </select>
          </div>

          <Input
            label="Description (Optional)"
            placeholder="Customer-facing portal and API endpoints"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

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
              Register System
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
