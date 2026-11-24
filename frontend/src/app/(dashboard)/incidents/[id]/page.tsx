'use client';

import React, { useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  AlertTriangle,
  CheckCircle2,
  Clock,
  UserCheck,
  Send,
  MessageSquare,
  History,
  Users,
  ShieldAlert,
} from 'lucide-react';
import { api } from '../../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../../components/ui/Card';
import { Button } from '../../../../components/ui/Button';
import { Badge } from '../../../../components/ui/Badge';
import { Modal } from '../../../../components/ui/Modal';

export default function IncidentDetailPage() {
  const params = useParams();
  const id = typeof params?.id === 'string' ? params.id : Array.isArray(params?.id) ? params.id[0] : '';
  const queryClient = useQueryClient();

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [isAssignTeamModalOpen, setIsAssignTeamModalOpen] = useState(false);
  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [commentText, setCommentText] = useState('');

  const { data: incident, isLoading } = useQuery({
    queryKey: ['incident-detail', id],
    queryFn: () => (id ? api.incidents.getById(id) : Promise.reject('No ID')),
    refetchInterval: 10000,
    enabled: !!id,
  });

  const { data: orgUsers } = useQuery({
    queryKey: ['organization-users'],
    queryFn: api.organization.getUsers,
  });

  const { data: teams = [] } = useQuery({
    queryKey: ['teams'],
    queryFn: api.teams.getAll,
  });

  const acknowledgeMutation = useMutation({
    mutationFn: () => (id ? api.incidents.acknowledge(id) : Promise.reject()),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
  });

  const assignMutation = useMutation({
    mutationFn: (userId: string) => (id ? api.incidents.assign(id, userId) : Promise.reject()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
      setIsAssignModalOpen(false);
    },
  });

  const assignTeamMutation = useMutation({
    mutationFn: (teamId: string) => (id ? api.incidents.assignTeam(id, teamId) : Promise.reject()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
      setIsAssignTeamModalOpen(false);
    },
  });

  const escalateMutation = useMutation({
    mutationFn: (reason?: string) => (id ? api.incidents.escalate(id, reason) : Promise.reject()),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
  });

  const resolveMutation = useMutation({
    mutationFn: (notes: string) => (id ? api.incidents.resolve(id, notes) : Promise.reject()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
      setIsResolveModalOpen(false);
    },
  });

  const commentMutation = useMutation({
    mutationFn: (comment: string) => (id ? api.incidents.addComment(id, comment) : Promise.reject()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incident-detail', id] }),
      setCommentText('');
    },
  });

  if (isLoading || !incident) {
    return <div className="text-center py-16 text-slate-400">Loading incident details...</div>;
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Back button and Header */}
      <div>
        <Link
          href="/incidents"
          className="inline-flex items-center text-sm font-medium text-slate-500 hover:text-slate-800 transition-colors mb-3"
        >
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          <span>Back to Incidents</span>
        </Link>
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <div>
            <div className="flex items-center space-x-2.5">
              <Badge
                variant={
                  incident.severity === 'CRITICAL'
                    ? 'destructive'
                    : incident.severity === 'HIGH'
                    ? 'warning'
                    : incident.severity === 'MEDIUM'
                    ? 'info'
                    : 'default'
                }
              >
                {incident.severity}
              </Badge>
              <h2 className="text-xl font-bold text-slate-900">{incident.title}</h2>
            </div>
            <p className="text-sm text-slate-500 mt-1">
              Triggered on <span className="font-semibold text-slate-700">{incident.systemName}</span> at{' '}
              {new Date(incident.createdAt).toLocaleString()}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {incident.status === 'OPEN' && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => acknowledgeMutation.mutate()}
                isLoading={acknowledgeMutation.isPending}
              >
                Acknowledge
              </Button>
            )}
            {incident.status !== 'RESOLVED' && incident.status !== 'CLOSED' && (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsAssignTeamModalOpen(true)}
                  className="space-x-1"
                >
                  <Users className="w-4 h-4 text-blue-600" />
                  <span>{incident.assignedTeamName ? 'Reassign Team' : 'Assign Team'}</span>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsAssignModalOpen(true)}
                  className="space-x-1"
                >
                  <UserCheck className="w-4 h-4" />
                  <span>{incident.assignedUserId ? 'Reassign Engineer' : 'Assign Engineer'}</span>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    if (confirm('Are you sure you want to escalate this incident to the next tier?')) {
                      escalateMutation.mutate('Manual escalation by incident responder');
                    }
                  }}
                  isLoading={escalateMutation.isPending}
                  className="border-red-200 text-red-600 hover:bg-red-50 space-x-1"
                >
                  <ShieldAlert className="w-4 h-4 text-red-600" />
                  <span>Escalate</span>
                </Button>
                <Button
                  variant="default"
                  size="sm"
                  onClick={() => setIsResolveModalOpen(true)}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white space-x-1"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Resolve Incident</span>
                </Button>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Incident Details Summary */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Current Status</span>
          <span
            className={`inline-block mt-1 px-2.5 py-0.5 rounded text-xs font-bold ${
              incident.status === 'OPEN'
                ? 'bg-red-100 text-red-800'
                : incident.status === 'ACKNOWLEDGED'
                ? 'bg-amber-100 text-amber-800'
                : incident.status === 'RESOLVED'
                ? 'bg-emerald-100 text-emerald-800'
                : 'bg-slate-100 text-slate-700'
            }`}
          >
            {incident.status}
          </span>
        </Card>
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Assigned Team</span>
          <span className="text-sm font-semibold text-slate-900 mt-1 block truncate">
            {incident.assignedTeamName || 'Unassigned'}
          </span>
        </Card>
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Assigned Engineer</span>
          <span className="text-sm font-semibold text-slate-900 mt-1 block truncate">
            {incident.assignedUserName || 'Unassigned'}
          </span>
        </Card>
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Escalation Tier</span>
          <div className="flex items-center space-x-1 mt-1">
            <span
              className={`px-2 py-0.5 rounded text-xs font-semibold ${
                (incident.escalationLevel ?? 1) > 1
                  ? 'bg-orange-100 text-orange-800'
                  : 'bg-slate-100 text-slate-700'
              }`}
            >
              Tier {incident.escalationLevel ?? 1}
            </span>
          </div>
        </Card>
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Acknowledged At</span>
          <span className="text-sm font-semibold text-slate-900 mt-1 block">
            {incident.acknowledgedAt ? new Date(incident.acknowledgedAt).toLocaleTimeString() : 'Pending'}
          </span>
        </Card>
        <Card className="p-4">
          <span className="text-xs text-slate-500 block">Resolved At</span>
          <span className="text-sm font-semibold text-slate-900 mt-1 block">
            {incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleTimeString() : 'Unresolved'}
          </span>
        </Card>
      </div>

      {incident.resolutionNotes && (
        <Card className="border-emerald-200 bg-emerald-50/40">
          <CardHeader>
            <CardTitle className="text-emerald-900 text-base">Resolution Summary</CardTitle>
          </CardHeader>
          <CardContent className="pt-0 text-sm text-emerald-800 font-mono">
            {incident.resolutionNotes}
          </CardContent>
        </Card>
      )}

      {/* Timeline & Comments 2-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Audit Timeline */}
        <Card>
          <CardHeader className="flex flex-row items-center space-x-2">
            <History className="w-5 h-5 text-blue-600" />
            <CardTitle className="text-base">Audit History & Timeline</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {incident.timeline && incident.timeline.length > 0 ? (
              <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                {incident.timeline.map((item) => (
                  <div key={item.id} className="relative">
                    <div className="absolute -left-[27px] top-1 w-3 h-3 rounded-full bg-blue-600 ring-4 ring-white" />
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-semibold text-xs text-slate-900 uppercase tracking-wide">
                          {item.action.replace('_', ' ')}
                        </span>
                        <span className="text-[11px] text-slate-400">
                          {new Date(item.createdAt).toLocaleTimeString()}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 mt-0.5">{item.details}</p>
                      <p className="text-[10px] text-slate-400 mt-0.5">By {item.userName}</p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400">No timeline history recorded.</p>
            )}
          </CardContent>
        </Card>

        {/* Comments & Collaboration Thread */}
        <Card className="flex flex-col h-full">
          <CardHeader className="flex flex-row items-center space-x-2">
            <MessageSquare className="w-5 h-5 text-blue-600" />
            <CardTitle className="text-base">Engineering Notes & Comments</CardTitle>
          </CardHeader>
          <CardContent className="flex-1 flex flex-col justify-between space-y-4">
            <div className="space-y-3 max-h-72 overflow-y-auto pr-2">
              {incident.comments && incident.comments.length > 0 ? (
                incident.comments.map((c) => (
                  <div key={c.id} className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
                    <div className="flex justify-between items-center mb-1">
                      <span className="font-semibold text-slate-800">{c.userName}</span>
                      <span className="text-slate-400">{new Date(c.createdAt).toLocaleTimeString()}</span>
                    </div>
                    <p className="text-slate-700 whitespace-pre-wrap">{c.comment}</p>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-400 italic">No comments yet. Add an update below.</p>
              )}
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (commentText.trim()) commentMutation.mutate(commentText);
              }}
              className="flex items-center space-x-2 pt-3 border-t border-slate-100"
            >
              <input
                type="text"
                placeholder="Post an investigation update or comment..."
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Button type="submit" size="sm" isLoading={commentMutation.isPending} className="space-x-1">
                <Send className="w-3.5 h-3.5" />
                <span>Post</span>
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>

      {/* Assign Team Modal */}
      <Modal
        isOpen={isAssignTeamModalOpen}
        onClose={() => setIsAssignTeamModalOpen(false)}
        title="Assign Incident to Team"
        description="Select an engineering team to own and resolve this incident"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Select Owning Team</label>
            <select
              value={selectedTeamId}
              onChange={(e) => setSelectedTeamId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Choose Engineering Team --</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.teamType})
                </option>
              ))}
            </select>
          </div>
          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAssignTeamModalOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!selectedTeamId}
              onClick={() => assignTeamMutation.mutate(selectedTeamId)}
              isLoading={assignTeamMutation.isPending}
            >
              Confirm Team Assignment
            </Button>
          </div>
        </div>
      </Modal>

      {/* Assign Modal */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Assign Incident to Engineer"
        description="Select an engineer within your organization"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Select Assignee</label>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Choose Engineer --</option>
              {orgUsers?.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.firstName} {u.lastName} ({u.role})
                </option>
              ))}
            </select>
          </div>
          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsAssignModalOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!selectedUserId}
              onClick={() => assignMutation.mutate(selectedUserId)}
              isLoading={assignMutation.isPending}
            >
              Confirm Assignment
            </Button>
          </div>
        </div>
      </Modal>

      {/* Resolve Modal */}
      <Modal
        isOpen={isResolveModalOpen}
        onClose={() => setIsResolveModalOpen(false)}
        title="Resolve Incident"
        description="Provide resolution notes and root cause summary"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Resolution Notes</label>
            <textarea
              rows={4}
              placeholder="e.g. Server restarted, memory leak mitigated, DNS records synchronized."
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsResolveModalOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!resolutionNotes.trim()}
              onClick={() => resolveMutation.mutate(resolutionNotes)}
              isLoading={resolveMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              Confirm Resolution
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
