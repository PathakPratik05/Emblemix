'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, Plus, Trash2, Mail } from 'lucide-react';
import { api, NotificationChannel } from '../../../lib/api';
import { Card, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { Badge } from '../../../components/ui/Badge';

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [channelType, setChannelType] = useState<'EMAIL' | 'SLACK' | 'SMS'>('EMAIL');
  const [destination, setDestination] = useState('');
  const [notifyCritical, setNotifyCritical] = useState(true);
  const [notifyHigh, setNotifyHigh] = useState(true);
  const [notifyResolve, setNotifyResolve] = useState(true);
  const [notifyAcknowledge, setNotifyAcknowledge] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: channels, isLoading } = useQuery({
    queryKey: ['notification-channels'],
    queryFn: api.notifications.getChannels,
  });

  const createMutation = useMutation({
    mutationFn: api.notifications.createChannel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-channels'] });
      setIsModalOpen(false);
      resetForm();
    },
    onError: (err: any) => setError(err.message || 'Failed to create channel'),
  });

  const deleteMutation = useMutation({
    mutationFn: api.notifications.deleteChannel,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-channels'] }),
  });

  const resetForm = () => {
    setChannelType('EMAIL');
    setDestination('');
    setNotifyCritical(true);
    setNotifyHigh(true);
    setNotifyResolve(true);
    setNotifyAcknowledge(false);
    setError(null);
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      channelType,
      destination,
      enabled: true,
      notifyOnCritical: notifyCritical,
      notifyOnHigh: notifyHigh,
      notifyOnResolve: notifyResolve,
      notifyOnAcknowledge: notifyAcknowledge,
    });
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Notification Channels</h2>
          <p className="text-sm text-slate-500 mt-1">
            Dispatch real-time incident notifications to on-call teams via SMTP Email and future integrations.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} className="space-x-1.5">
          <Plus className="w-4 h-4" />
          <span>Add Channel</span>
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Channel</th>
                  <th className="py-3 px-6">Destination</th>
                  <th className="py-3 px-6">Triggers</th>
                  <th className="py-3 px-6">Status</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-10 text-slate-400">
                      Loading notification channels...
                    </td>
                  </tr>
                ) : channels && channels.length > 0 ? (
                  channels.map((ch) => (
                    <tr key={ch.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-6 font-semibold text-slate-900 flex items-center space-x-2">
                        <Mail className="w-4 h-4 text-blue-600" />
                        <span>{ch.channelType}</span>
                      </td>
                      <td className="py-4 px-6 font-mono text-xs text-slate-800">{ch.destination}</td>
                      <td className="py-4 px-6 text-xs text-slate-600 space-x-1">
                        {ch.notifyOnCritical && <Badge variant="destructive">Critical</Badge>}
                        {ch.notifyOnHigh && <Badge variant="warning">High</Badge>}
                        {ch.notifyOnResolve && <Badge variant="success">Resolve</Badge>}
                        {ch.notifyOnAcknowledge && <Badge variant="info">Ack</Badge>}
                      </td>
                      <td className="py-4 px-6">
                        <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">
                          Enabled
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            if (window.confirm(`Delete notification channel ${ch.destination}?`)) {
                              deleteMutation.mutate(ch.id);
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
                    <td colSpan={5} className="text-center py-12 text-slate-400">
                      No notification channels configured yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Add Channel Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          resetForm();
        }}
        title="Add Notification Channel"
        description="Configure recipient and alert dispatch criteria"
      >
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {error}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Channel Type</label>
            <select
              value={channelType}
              onChange={(e) => setChannelType(e.target.value as any)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="EMAIL">Email (SMTP)</option>
              <option value="SLACK">Slack Webhook (Future)</option>
              <option value="SMS">SMS / Pager (Future)</option>
            </select>
          </div>

          <Input
            label="Destination (Email Address)"
            type="email"
            placeholder="oncall-engineers@acme.com"
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            required
          />

          <div className="space-y-2 pt-2 border-t border-slate-100 text-sm">
            <span className="font-medium text-slate-700 block mb-1">Dispatch Alerts On:</span>
            <div className="flex items-center space-x-2">
              <input
                id="crit"
                type="checkbox"
                checked={notifyCritical}
                onChange={(e) => setNotifyCritical(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="crit" className="text-slate-700">Critical Severity Incidents</label>
            </div>
            <div className="flex items-center space-x-2">
              <input
                id="high"
                type="checkbox"
                checked={notifyHigh}
                onChange={(e) => setNotifyHigh(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="high" className="text-slate-700">High Severity Incidents</label>
            </div>
            <div className="flex items-center space-x-2">
              <input
                id="res"
                type="checkbox"
                checked={notifyResolve}
                onChange={(e) => setNotifyResolve(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="res" className="text-slate-700">Incident Resolution & Recovery</label>
            </div>
            <div className="flex items-center space-x-2">
              <input
                id="ack"
                type="checkbox"
                checked={notifyAcknowledge}
                onChange={(e) => setNotifyAcknowledge(e.target.checked)}
                className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <label htmlFor="ack" className="text-slate-700">Incident Acknowledgements</label>
            </div>
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
              Save Channel
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
