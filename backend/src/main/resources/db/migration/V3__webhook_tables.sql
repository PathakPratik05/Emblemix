-- ====================================================================
-- Flyway Migration V3: Custom Webhook Integration Schema
-- ====================================================================

CREATE TABLE IF NOT EXISTS webhook_configurations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    system_id UUID REFERENCES connected_systems(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    encrypted_secret VARCHAR(512) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_config_org ON webhook_configurations(organization_id);
CREATE INDEX IF NOT EXISTS idx_webhook_config_system ON webhook_configurations(system_id);

CREATE TABLE IF NOT EXISTS webhook_delivery_logs (
    id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL REFERENCES webhook_configurations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    http_status_code INT NOT NULL,
    error_message TEXT,
    payload_summary VARCHAR(1000),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_logs_webhook_time ON webhook_delivery_logs(webhook_id, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_org_time ON webhook_delivery_logs(organization_id, received_at DESC);
