'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, Team, TeamType, UserProfile } from '../../../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import {
  Users,
  Plus,
  Shield,
  Trash2,
  UserPlus,
  UserCheck,
  Server,
  AlertCircle,
  CheckCircle2,
} from 'lucide-react';

export default function TeamsPage() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);

  // Form states
  const [name, setName] = useState('');
  const [teamType, setTeamType] = useState<TeamType>('DEVOPS');
  const [description, setDescription] = useState('');
  const [leadId, setLeadId] = useState('');
  const [memberUserId, setMemberUserId] = useState('');
  const [memberRole, setMemberRole] = useState<'MEMBER' | 'LEAD'>('MEMBER');
  const [errorMsg, setErrorMsg] = useState('');

  // Queries
  const { data: teams = [], isLoading } = useQuery({
    queryKey: ['teams'],
    queryFn: () => api.teams.getAll(),
  });

  const { data: users = [] } = useQuery({
    queryKey: ['org-users'],
    queryFn: () => api.organization.getUsers(),
  });

  // Mutations
  const createTeamMutation = useMutation({
    mutationFn: api.teams.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      setShowCreateModal(false);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to create team');
    },
  });

  const deleteTeamMutation = useMutation({
    mutationFn: api.teams.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });

  const addMemberMutation = useMutation({
    mutationFn: ({ teamId, payload }: { teamId: string; payload: { userId: string; role: 'LEAD' | 'MEMBER' } }) =>
      api.teams.addMember(teamId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      setShowAddMemberModal(false);
      setMemberUserId('');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to add member');
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: ({ teamId, userId }: { teamId: string; userId: string }) =>
      api.teams.removeMember(teamId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });

  const resetForm = () => {
    setName('');
    setTeamType('DEVOPS');
    setDescription('');
    setLeadId('');
    setErrorMsg('');
  };

  const handleCreateTeam = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    createTeamMutation.mutate({
      name: name.trim(),
      teamType,
      description: description.trim() || undefined,
      leadId: leadId || undefined,
    });
  };

  const handleAddMember = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTeam || !memberUserId) return;
    addMemberMutation.mutate({
      teamId: selectedTeam.id,
      payload: { userId: memberUserId, role: memberRole },
    });
  };

  const teamTypeColors: Record<TeamType, string> = {
    DEVOPS: 'bg-purple-100 text-purple-800 border-purple-200',
    SALESFORCE: 'bg-sky-100 text-sky-800 border-sky-200',
    WEB_ENGINEERING: 'bg-blue-100 text-blue-800 border-blue-200',
    DATA_ANALYTICS: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    APPLICATION_ENGINEERING: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    SECURITY: 'bg-red-100 text-red-800 border-red-200',
    SUPPORT: 'bg-amber-100 text-amber-800 border-amber-200',
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Teams & Collaboration</h1>
          <p className="text-sm text-slate-500">
            Organize multi-disciplinary engineering specialists, assign system ownership, and route incidents automatically.
          </p>
        </div>
        <Button onClick={() => setShowCreateModal(true)} className="flex items-center space-x-2">
          <Plus className="w-4 h-4" />
          <span>Create Team</span>
        </Button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Total Teams</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{teams.length}</p>
            </div>
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-lg">
              <Users className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Team Members</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">
                {teams.reduce((acc, t) => acc + (t.memberCount || 0), 0)}
              </p>
            </div>
            <div className="p-2.5 bg-purple-50 text-purple-600 rounded-lg">
              <UserCheck className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Organizations Users</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">{users.length}</p>
            </div>
            <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
              <Shield className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase">Disciplines Active</p>
              <p className="text-2xl font-bold text-slate-900 mt-1">
                {new Set(teams.map((t) => t.teamType)).size}
              </p>
            </div>
            <div className="p-2.5 bg-amber-50 text-amber-600 rounded-lg">
              <Server className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Teams Grid */}
      {isLoading ? (
        <div className="p-8 text-center text-slate-400">Loading engineering teams...</div>
      ) : teams.length === 0 ? (
        <Card className="p-12 text-center">
          <Users className="w-12 h-12 text-slate-300 mx-auto mb-4" />
          <h3 className="text-base font-semibold text-slate-800">No teams created yet</h3>
          <p className="text-sm text-slate-500 max-w-sm mx-auto mt-1 mb-4">
            Create engineering teams to establish system ownership and automate incident escalations.
          </p>
          <Button onClick={() => setShowCreateModal(true)}>Create Your First Team</Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {teams.map((team) => (
            <Card key={team.id} className="flex flex-col justify-between hover:shadow-md transition-shadow">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <CardTitle className="text-base font-bold text-slate-900">{team.name}</CardTitle>
                    <span
                      className={`inline-block mt-1 text-[11px] font-semibold px-2 py-0.5 rounded border ${
                        teamTypeColors[team.teamType] || 'bg-slate-100 text-slate-800'
                      }`}
                    >
                      {team.teamType.replace('_', ' ')}
                    </span>
                  </div>
                  <button
                    onClick={() => {
                      if (confirm(`Are you sure you want to delete team '${team.name}'?`)) {
                        deleteTeamMutation.mutate(team.id);
                      }
                    }}
                    className="p-1 text-slate-400 hover:text-red-600 rounded transition-colors"
                    title="Delete Team"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
                {team.description && (
                  <p className="text-xs text-slate-500 mt-2 line-clamp-2">{team.description}</p>
                )}
              </CardHeader>

              <CardContent className="pt-3 space-y-4">
                {/* Team Lead */}
                <div className="text-xs">
                  <span className="font-semibold text-slate-500">Team Lead: </span>
                  <span className="text-slate-800 font-medium">
                    {team.leadName || 'Unassigned'}
                  </span>
                </div>

                {/* Members List */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold text-slate-700">
                      Members ({team.members?.length || 0})
                    </span>
                    <button
                      onClick={() => {
                        setSelectedTeam(team);
                        setShowAddMemberModal(true);
                      }}
                      className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center space-x-1"
                    >
                      <UserPlus className="w-3.5 h-3.5" />
                      <span>Add</span>
                    </button>
                  </div>

                  <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                    {team.members && team.members.length > 0 ? (
                      team.members.map((member) => (
                        <div
                          key={member.userId}
                          className="flex items-center justify-between text-xs p-1.5 rounded bg-slate-50 border border-slate-100"
                        >
                          <div className="truncate mr-2">
                            <span className="font-medium text-slate-900">
                              {member.firstName} {member.lastName}
                            </span>
                            <span className="text-slate-400 block text-[10px] truncate">{member.email}</span>
                          </div>
                          <div className="flex items-center space-x-1 flex-shrink-0">
                            {member.role === 'LEAD' && (
                              <span className="text-[10px] bg-amber-50 text-amber-700 border border-amber-200 px-1 py-0.5 rounded font-bold">
                                LEAD
                              </span>
                            )}
                            <button
                              onClick={() =>
                                removeMemberMutation.mutate({ teamId: team.id, userId: member.userId })
                              }
                              className="text-slate-400 hover:text-red-500 p-0.5"
                              title="Remove member"
                            >
                              &times;
                            </button>
                          </div>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-slate-400 italic">No members added yet.</p>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create Team Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-slate-900">Create Collaborative Team</h3>

            {errorMsg && (
              <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleCreateTeam} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Team Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. SRE / Cloud Platform"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Team Discipline Type *</label>
                <select
                  value={teamType}
                  onChange={(e) => setTeamType(e.target.value as TeamType)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="DEVOPS">DevOps / SRE / Infrastructure</option>
                  <option value="SALESFORCE">Salesforce Engineering</option>
                  <option value="WEB_ENGINEERING">Web Engineering</option>
                  <option value="DATA_ANALYTICS">Data & Analytics / Power BI</option>
                  <option value="APPLICATION_ENGINEERING">Application Engineering</option>
                  <option value="SECURITY">Security / SecOps</option>
                  <option value="SUPPORT">Tier 2/3 Technical Support</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Team Lead (Optional)</label>
                <select
                  value={leadId}
                  onChange={(e) => setLeadId(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">-- Select Team Lead --</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName} ({u.email})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Responsibilities, scope, and on-call rotation..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <Button type="button" variant="outline" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={createTeamMutation.isPending}>
                  {createTeamMutation.isPending ? 'Creating...' : 'Create Team'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Member Modal */}
      {showAddMemberModal && selectedTeam && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-sm w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-slate-900">Add Member to {selectedTeam.name}</h3>

            {errorMsg && (
              <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleAddMember} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Organization User *</label>
                <select
                  required
                  value={memberUserId}
                  onChange={(e) => setMemberUserId(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="">-- Select User --</option>
                  {users
                    .filter((u) => !selectedTeam.members?.some((m) => m.userId === u.id))
                    .map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.firstName} {u.lastName} ({u.email})
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Team Role</label>
                <select
                  value={memberRole}
                  onChange={(e) => setMemberRole(e.target.value as 'MEMBER' | 'LEAD')}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  <option value="MEMBER">Member</option>
                  <option value="LEAD">Team Lead</option>
                </select>
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <Button type="button" variant="outline" onClick={() => setShowAddMemberModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={addMemberMutation.isPending}>
                  {addMemberMutation.isPending ? 'Adding...' : 'Add Member'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
