-- ====================================================================
-- Flyway Migration V1: Initial Database Schema for Emblemix Platform
-- ====================================================================

-- 1. Organizations
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);

-- 2. Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_org ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 3. Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

-- 4. Password Reset Tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_tokens(token);

-- 5. Connected Systems
CREATE TABLE IF NOT EXISTS connected_systems (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    system_type VARCHAR(50) NOT NULL,
    description TEXT,
    environment VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',
    status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_monitored_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_connected_systems_org ON connected_systems(organization_id);
CREATE INDEX IF NOT EXISTS idx_connected_systems_status ON connected_systems(organization_id, status);

-- 6. Website Configurations
CREATE TABLE IF NOT EXISTS website_configurations (
    id UUID PRIMARY KEY,
    system_id UUID NOT NULL UNIQUE REFERENCES connected_systems(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    monitoring_interval_seconds INT NOT NULL DEFAULT 60,
    request_timeout_ms INT NOT NULL DEFAULT 5000,
    expected_http_status_min INT NOT NULL DEFAULT 200,
    expected_http_status_max INT NOT NULL DEFAULT 299,
    expected_response_content TEXT,
    check_ssl BOOLEAN NOT NULL DEFAULT TRUE,
    ssl_expiry_threshold_days INT NOT NULL DEFAULT 30,
    consecutive_failure_threshold INT NOT NULL DEFAULT 2,
    current_consecutive_failures INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_website_config_org ON website_configurations(organization_id);

-- 7. Integration Credentials (Tokens Encrypted at Rest)
CREATE TABLE IF NOT EXISTS integration_credentials (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID NOT NULL REFERENCES connected_systems(id) ON DELETE CASCADE,
    integration_type VARCHAR(50) NOT NULL,
    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,
    instance_url VARCHAR(512),
    external_org_id VARCHAR(255),
    token_type VARCHAR(50),
    issued_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_integration_system_type UNIQUE (system_id, integration_type)
);
CREATE INDEX IF NOT EXISTS idx_credentials_org ON integration_credentials(organization_id);

-- 8. Salesforce Configurations
CREATE TABLE IF NOT EXISTS salesforce_configurations (
    id UUID PRIMARY KEY,
    system_id UUID NOT NULL UNIQUE REFERENCES connected_systems(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    is_sandbox BOOLEAN NOT NULL DEFAULT FALSE,
    api_usage_warning_threshold_pct DOUBLE PRECISION NOT NULL DEFAULT 80.0,
    api_usage_critical_threshold_pct DOUBLE PRECISION NOT NULL DEFAULT 95.0,
    monitor_apex_jobs BOOLEAN NOT NULL DEFAULT TRUE,
    monitor_login_activity BOOLEAN NOT NULL DEFAULT TRUE,
    last_apex_job_checked_at TIMESTAMP WITH TIME ZONE,
    last_login_checked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sf_config_org ON salesforce_configurations(organization_id);

-- 9. Monitoring Results
CREATE TABLE IF NOT EXISTS monitoring_results (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID NOT NULL REFERENCES connected_systems(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    http_status_code INT,
    response_time_ms BIGINT,
    ssl_valid BOOLEAN,
    ssl_days_remaining INT,
    error_message TEXT,
    raw_metrics_json TEXT,
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_monitoring_results_sys_time ON monitoring_results(system_id, checked_at DESC);
CREATE INDEX IF NOT EXISTS idx_monitoring_results_org_time ON monitoring_results(organization_id, checked_at DESC);

-- 10. Events
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID NOT NULL REFERENCES connected_systems(id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    details_json TEXT,
    fingerprint VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_events_org_time ON events(organization_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_fingerprint ON events(fingerprint);
CREATE INDEX IF NOT EXISTS idx_events_system ON events(system_id);

-- 11. Alert Rules
CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID REFERENCES connected_systems(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    target_severity VARCHAR(50) NOT NULL,
    failure_count_threshold INT NOT NULL DEFAULT 1,
    response_time_threshold_ms BIGINT,
    api_usage_pct_threshold DOUBLE PRECISION,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_alert_rules_org ON alert_rules(organization_id);

-- 12. Incidents
CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID NOT NULL REFERENCES connected_systems(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    source_event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    fingerprint VARCHAR(255) NOT NULL,
    assigned_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    resolution_notes TEXT,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_incidents_org_status ON incidents(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_incidents_fingerprint ON incidents(organization_id, fingerprint, status);
CREATE INDEX IF NOT EXISTS idx_incidents_system ON incidents(system_id);

-- 13. Incident Events Link
CREATE TABLE IF NOT EXISTS incident_events (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_incident_event UNIQUE (incident_id, event_id)
);
CREATE INDEX IF NOT EXISTS idx_incident_events_incident ON incident_events(incident_id);

-- 14. Incident History (Audit & Timeline)
CREATE TABLE IF NOT EXISTS incident_history (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_incident_history_inc ON incident_history(incident_id, created_at ASC);

-- 15. Incident Comments
CREATE TABLE IF NOT EXISTS incident_comments (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_incident_comments_inc ON incident_comments(incident_id, created_at ASC);

-- 16. Notification Channels
CREATE TABLE IF NOT EXISTS notification_channels (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    channel_type VARCHAR(50) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_critical BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_high BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_resolve BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_acknowledge BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notification_channels_org ON notification_channels(organization_id);

-- 17. Notification Logs
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    channel_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notification_logs_inc ON notification_logs(incident_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_org ON notification_logs(organization_id);

-- 18. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    details_json TEXT,
    ip_address VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_time ON audit_logs(organization_id, created_at DESC);
