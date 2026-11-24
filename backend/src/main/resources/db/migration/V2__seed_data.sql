-- ====================================================================
-- Flyway Migration V2: Development Seed Data
-- ====================================================================

-- 1. Default Acme Demo Organization
INSERT INTO organizations (id, name, slug, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Acme Global Enterprises',
    'acme-global',
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 2. Seed Users
-- Default Seed Password for all seed accounts: "Password123!"
-- BCrypt Hash: $2a$10$wT0l2mI/C8V1M7hC2s5f4eKkJ9q8vN7lP6t5r4e3w2q1a0b9c8d7e (re-verified at application startup)
INSERT INTO users (id, organization_id, email, password_hash, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'admin@emblemix.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- standard bcrypt for password 'secret', updated on startup to Password123!
    'Alex',
    'Admin',
    'ORG_ADMIN',
    TRUE,
    NOW(),
    NOW()
),
(
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'engineer@emblemix.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Elena',
    'Engineer',
    'ENGINEER',
    TRUE,
    NOW(),
    NOW()
),
(
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'viewer@emblemix.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Victor',
    'Viewer',
    'VIEWER',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- 3. Default Notification Channel for Org
INSERT INTO notification_channels (id, organization_id, channel_type, destination, enabled, notify_on_critical, notify_on_high, notify_on_resolve, notify_on_acknowledge, created_at)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'EMAIL',
    'ops-team@emblemix.com',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 4. Default Connected System: Acme Public Website
INSERT INTO connected_systems (id, organization_id, name, system_type, description, environment, status, enabled, last_monitored_at, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'Acme Main Portal',
    'WEBSITE',
    'Primary customer facing marketing and portal website',
    'PRODUCTION',
    'HEALTHY',
    TRUE,
    NOW(),
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO website_configurations (id, system_id, organization_id, url, monitoring_interval_seconds, request_timeout_ms, expected_http_status_min, expected_http_status_max, check_ssl, ssl_expiry_threshold_days, consecutive_failure_threshold, current_consecutive_failures, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'https://httpbin.org/status/200',
    60,
    5000,
    200,
    299,
    TRUE,
    30,
    2,
    0,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 5. Default Alert Rules for Org
INSERT INTO alert_rules (id, organization_id, system_id, name, event_type, target_severity, failure_count_threshold, response_time_threshold_ms, api_usage_pct_threshold, enabled, created_at, updated_at)
VALUES
(
    'f0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    NULL,
    'Default Website Down Rule',
    'WEBSITE_DOWN',
    'CRITICAL',
    2,
    NULL,
    NULL,
    TRUE,
    NOW(),
    NOW()
),
(
    'f0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    NULL,
    'High Latency Rule (>2000ms)',
    'HIGH_RESPONSE_TIME',
    'MEDIUM',
    2,
    2000,
    NULL,
    TRUE,
    NOW(),
    NOW()
),
(
    'f0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    NULL,
    'Salesforce API Critical Limit (>95%)',
    'SALESFORCE_API_LIMIT_CRITICAL',
    'CRITICAL',
    1,
    NULL,
    95.0,
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;
