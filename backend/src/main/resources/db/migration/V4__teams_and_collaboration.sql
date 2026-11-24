-- ====================================================================
-- Flyway Migration V4: Teams, Ownership & Collaboration Extension
-- ====================================================================

-- 1. Organization Teams Table
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    team_type VARCHAR(50) NOT NULL,
    description TEXT,
    lead_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_org_team_name UNIQUE (organization_id, name)
);

CREATE INDEX IF NOT EXISTS idx_teams_org ON teams(organization_id);
CREATE INDEX IF NOT EXISTS idx_teams_type ON teams(team_type);

-- 2. Team Members Table (Many-to-Many: User can belong to multiple teams)
CREATE TABLE IF NOT EXISTS team_members (
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (team_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);

-- 3. Extend connected_systems for granular ownership & criticality
ALTER TABLE connected_systems
    ADD COLUMN IF NOT EXISTS owning_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS primary_owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS backup_owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS criticality VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS escalation_policy VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_conn_systems_team ON connected_systems(owning_team_id);
CREATE INDEX IF NOT EXISTS idx_conn_systems_owner ON connected_systems(primary_owner_id);

-- 4. Extend incidents for team routing & escalation
ALTER TABLE incidents
    ADD COLUMN IF NOT EXISTS assigned_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS escalation_level INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_incidents_assigned_team ON incidents(assigned_team_id);
