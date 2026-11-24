export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface UserProfile {
  id: string;
  organizationId: string;
  organizationName: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'SUPER_ADMIN' | 'ORG_ADMIN' | 'ENGINEER' | 'VIEWER';
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface ConnectedSystem {
  id: string;
  organizationId: string;
  name: string;
  systemType: 'WEBSITE' | 'SALESFORCE' | 'WEBHOOK';
  description?: string;
  environment: string;
  status: 'HEALTHY' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';
  enabled: boolean;
  lastMonitoredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WebsiteConfiguration {
  id: string;
  systemId: string;
  name: string;
  url: string;
  environment: string;
  status: 'HEALTHY' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';
  enabled: boolean;
  monitoringIntervalSeconds: number;
  requestTimeoutMs: number;
  expectedHttpStatusMin: number;
  expectedHttpStatusMax: number;
  expectedResponseContent?: string;
  checkSsl: boolean;
  sslExpiryThresholdDays: number;
  consecutiveFailureThreshold: number;
  currentConsecutiveFailures: number;
  lastMonitoredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MonitoringResult {
  id: string;
  systemId: string;
  status: 'SUCCESS' | 'FAILURE' | 'WARNING';
  httpStatusCode?: number;
  responseTimeMs?: number;
  sslValid?: boolean;
  sslDaysRemaining?: number;
  errorMessage?: string;
  checkedAt: string;
}

export interface SalesforceConfig {
  id: string;
  systemId: string;
  name: string;
  isSandbox: boolean;
  apiUsageWarningThresholdPct: number;
  apiUsageCriticalThresholdPct: number;
  monitorApexJobs: boolean;
  monitorLoginActivity: boolean;
  environment: string;
  status: 'HEALTHY' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';
  enabled: boolean;
  connected: boolean;
  instanceUrl?: string;
  externalOrgId?: string;
  lastApexJobCheckedAt?: string;
  lastLoginCheckedAt?: string;
  lastMonitoredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Incident {
  id: string;
  organizationId: string;
  systemId: string;
  systemName: string;
  title: string;
  description?: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'ACKNOWLEDGED' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';
  fingerprint: string;
  assignedUserId?: string;
  assignedUserName?: string;
  resolutionNotes?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface IncidentTimeline {
  id: string;
  action: string;
  fromStatus?: string;
  toStatus?: string;
  details?: string;
  userName?: string;
  createdAt: string;
}

export interface IncidentComment {
  id: string;
  comment: string;
  userId: string;
  userName: string;
  createdAt: string;
}

export interface IncidentDetail extends Incident {
  timeline: IncidentTimeline[];
  comments: IncidentComment[];
}

export interface AlertRule {
  id: string;
  organizationId: string;
  systemId?: string;
  systemName: string;
  name: string;
  eventType: string;
  targetSeverity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  failureCountThreshold: number;
  responseTimeThresholdMs?: number;
  apiUsagePctThreshold?: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationChannel {
  id: string;
  organizationId: string;
  channelType: 'EMAIL' | 'SLACK' | 'SMS';
  destination: string;
  enabled: boolean;
  notifyOnCritical: boolean;
  notifyOnHigh: boolean;
  notifyOnResolve: boolean;
  notifyOnAcknowledge: boolean;
  createdAt: string;
}

export interface DashboardOverview {
  totalSystems: number;
  healthySystems: number;
  degradedSystems: number;
  downSystems: number;
  openIncidents: number;
  criticalIncidents: number;
  resolvedTodayIncidents: number;
  averageResolutionTimeMinutes: number;
  websiteUptimePercentage: number;
  averageResponseTimeMs: number;
  systemHealthDistribution: Record<string, number>;
  incidentSeverityDistribution: Record<string, number>;
}

export interface IncidentTrend {
  date: string;
  count: number;
  resolved: number;
}

export interface UptimeReport {
  systemId: string;
  systemName: string;
  uptimePercentage: number;
  averageResponseTimeMs: number;
  totalChecks: number;
  successfulChecks: number;
  failedChecks: number;
}

export interface WebhookConfiguration {
  id: string;
  organizationId: string;
  systemId?: string;
  systemName?: string;
  name: string;
  description?: string;
  webhookUrl: string;
  secret?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDeliveryLog {
  id: string;
  webhookId: string;
  status: 'SUCCESS' | 'INVALID_SIGNATURE' | 'INVALID_PAYLOAD' | 'PROCESSING_ERROR' | 'SYSTEM_DISABLED';
  httpStatusCode: number;
  errorMessage?: string;
  payloadSummary?: string;
  receivedAt: string;
}

export interface WebhookIngestResult {
  status: string;
  message: string;
  eventId?: string;
}

const API_BASE = '/api/v1';

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('emblemix_token') : null;
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 && !endpoint.includes('/auth/')) {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('emblemix_token');
      localStorage.removeItem('emblemix_user');
      window.location.href = '/login';
    }
    throw new Error('Session expired, please log in again.');
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMsg = data?.message || data?.error || `HTTP error ${response.status}`;
    throw new Error(errorMsg);
  }

  return (data as ApiResponse<T>).data;
}

export const api = {
  auth: {
    login: (credentials: { email: string; password: string }) =>
      request<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
      }),
    register: (payload: {
      organizationName: string;
      firstName: string;
      lastName: string;
      email: string;
      password: string;
    }) =>
      request<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    forgotPassword: (email: string) =>
      request<{ message: string; resetToken?: string }>('/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      }),
    resetPassword: (payload: { token: string; newPassword: string }) =>
      request<void>('/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    getProfile: () => request<UserProfile>('/users/me'),
  },

  dashboard: {
    getOverview: () => request<DashboardOverview>('/reports/overview'),
    getIncidentTrends: (days = 14) => request<IncidentTrend[]>(`/reports/incidents?days=${days}`),
    getUptimeReports: (days = 30) => request<UptimeReport[]>(`/reports/uptime?days=${days}`),
  },

  systems: {
    getAll: (type?: string, page = 0, size = 50) =>
      request<PagedResponse<ConnectedSystem>>(
        `/systems?page=${page}&size=${size}${type ? `&type=${type}` : ''}`
      ),
    getById: (id: string) => request<ConnectedSystem>(`/systems/${id}`),
    create: (payload: { name: string; systemType: string; description?: string; environment?: string }) =>
      request<ConnectedSystem>('/systems', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    update: (id: string, payload: Partial<ConnectedSystem>) =>
      request<ConnectedSystem>(`/systems/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
    delete: (id: string) =>
      request<void>(`/systems/${id}`, {
        method: 'DELETE',
      }),
  },

  websites: {
    getAll: (page = 0, size = 50) =>
      request<PagedResponse<WebsiteConfiguration>>(`/websites?page=${page}&size=${size}`),
    getById: (id: string) => request<WebsiteConfiguration>(`/websites/${id}`),
    create: (payload: any) =>
      request<WebsiteConfiguration>('/websites', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    update: (id: string, payload: any) =>
      request<WebsiteConfiguration>(`/websites/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
    test: (id: string) =>
      request<{
        healthy: boolean;
        httpStatusCode?: number;
        responseTimeMs?: number;
        sslValid?: boolean;
        sslDaysRemaining?: number;
        message: string;
        error?: string;
      }>(`/websites/${id}/test`, {
        method: 'POST',
      }),
    getHistory: (id: string, page = 0, size = 30) =>
      request<PagedResponse<MonitoringResult>>(`/websites/${id}/monitoring-history?page=${page}&size=${size}`),
  },

  salesforce: {
    getAll: () => request<SalesforceConfig[]>('/integrations/salesforce'),
    create: (payload: any) =>
      request<SalesforceConfig>('/integrations/salesforce', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    getAuthUrl: (systemId: string, isSandbox = false) =>
      request<{ authorizationUrl: string; state: string }>(
        `/integrations/salesforce/authorize?systemId=${systemId}&isSandbox=${isSandbox}`
      ),
    test: (id: string) =>
      request<{
        connected: boolean;
        instanceUrl?: string;
        externalOrgId?: string;
        limits?: { max: number; remaining: number; percentageUsed: number };
        failedApexJobs?: any[];
        message: string;
        error?: string;
      }>(`/integrations/salesforce/${id}/test`, {
        method: 'POST',
      }),
    disconnect: (id: string) =>
      request<void>(`/integrations/salesforce/${id}/disconnect`, {
        method: 'POST',
      }),
  },

  incidents: {
    getAll: (params?: { status?: string; severity?: string; page?: number; size?: number }) => {
      const q = new URLSearchParams();
      if (params?.status) q.set('status', params.status);
      if (params?.severity) q.set('severity', params.severity);
      q.set('page', String(params?.page ?? 0));
      q.set('size', String(params?.size ?? 50));
      return request<PagedResponse<Incident>>(`/incidents?${q.toString()}`);
    },
    getById: (id: string) => request<IncidentDetail>(`/incidents/${id}`),
    acknowledge: (id: string) =>
      request<Incident>(`/incidents/${id}/acknowledge`, {
        method: 'POST',
      }),
    assign: (id: string, userId: string) =>
      request<Incident>(`/incidents/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify({ userId }),
      }),
    resolve: (id: string, resolutionNotes: string) =>
      request<Incident>(`/incidents/${id}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ resolutionNotes }),
      }),
    addComment: (id: string, comment: string) =>
      request<IncidentComment>(`/incidents/${id}/comments`, {
        method: 'POST',
        body: JSON.stringify({ comment }),
      }),
  },

  alertRules: {
    getAll: () => request<AlertRule[]>('/alert-rules'),
    create: (payload: any) =>
      request<AlertRule>('/alert-rules', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    update: (id: string, payload: any) =>
      request<AlertRule>(`/alert-rules/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
    delete: (id: string) =>
      request<void>(`/alert-rules/${id}`, {
        method: 'DELETE',
      }),
  },

  notifications: {
    getChannels: () => request<NotificationChannel[]>('/notifications/channels'),
    createChannel: (payload: any) =>
      request<NotificationChannel>('/notifications/channels', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    deleteChannel: (id: string) =>
      request<void>(`/notifications/channels/${id}`, {
        method: 'DELETE',
      }),
  },

  organization: {
    getCurrent: () => request<{ id: string; name: string; slug: string }>('/organizations/current'),
    getUsers: () => request<UserProfile[]>('/users'),
  },

  webhooks: {
    getAll: () => request<WebhookConfiguration[]>('/webhooks'),
    getById: (id: string) => request<WebhookConfiguration>(`/webhooks/${id}`),
    create: (payload: { name: string; description?: string; systemId?: string }) =>
      request<WebhookConfiguration>('/webhooks', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    rotateSecret: (id: string) =>
      request<WebhookConfiguration>(`/webhooks/${id}/rotate-secret`, {
        method: 'POST',
      }),
    delete: (id: string) =>
      request<void>(`/webhooks/${id}`, {
        method: 'DELETE',
      }),
    getDeliveries: (id: string, page = 0, size = 20) =>
      request<WebhookDeliveryLog[]>(`/webhooks/${id}/deliveries?page=${page}&size=${size}`),
    test: (id: string) =>
      request<WebhookIngestResult>(`/webhooks/${id}/test`, {
        method: 'POST',
      }),
  },
};
