package com.emblemix.system;

import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.system.dto.ConnectedSystemRequest;
import com.emblemix.system.dto.ConnectedSystemResponse;
import com.emblemix.team.Team;
import com.emblemix.team.TeamRepository;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConnectedSystemService {

    private final ConnectedSystemRepository systemRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public ConnectedSystemService(ConnectedSystemRepository systemRepository,
                                  OrganizationRepository organizationRepository,
                                  TeamRepository teamRepository,
                                  UserRepository userRepository) {
        this.systemRepository = systemRepository;
        this.organizationRepository = organizationRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConnectedSystemResponse createSystem(ConnectedSystemRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();

        if (systemRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, request.name().trim())) {
            throw new BadRequestException("A system with the name '" + request.name() + "' already exists in your organization");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        ConnectedSystem system = new ConnectedSystem(
                UUID.randomUUID(),
                org,
                request.name().trim(),
                request.systemType(),
                request.description(),
                request.environment() != null ? request.environment() : "PRODUCTION"
        );
        if (request.enabled() != null) {
            system.setEnabled(request.enabled());
        }
        if (request.criticality() != null) {
            system.setCriticality(request.criticality());
        }
        if (request.escalationPolicy() != null) {
            system.setEscalationPolicy(request.escalationPolicy());
        }
        if (request.owningTeamId() != null) {
            Team team = teamRepository.findByIdAndOrganizationId(request.owningTeamId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.owningTeamId()));
            system.setOwningTeam(team);
        }
        if (request.primaryOwnerId() != null) {
            User primary = userRepository.findById(request.primaryOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.primaryOwnerId()));
            if (!primary.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Primary owner must belong to your organization");
            }
            system.setPrimaryOwner(primary);
        }
        if (request.backupOwnerId() != null) {
            User backup = userRepository.findById(request.backupOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.backupOwnerId()));
            if (!backup.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Backup owner must belong to your organization");
            }
            system.setBackupOwner(backup);
        }

        return ConnectedSystemResponse.from(systemRepository.save(system));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConnectedSystemResponse> getSystems(SystemType systemType, Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<ConnectedSystem> page;
        if (systemType != null) {
            page = systemRepository.findByOrganizationIdAndSystemType(orgId, systemType, pageable);
        } else {
            page = systemRepository.findByOrganizationId(orgId, pageable);
        }

        return new PagedResponse<>(
                page.map(ConnectedSystemResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ConnectedSystemResponse getSystemById(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", id));
        return ConnectedSystemResponse.from(system);
    }

    @Transactional
    public ConnectedSystemResponse updateSystem(UUID id, ConnectedSystemRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", id));

        system.setName(request.name().trim());
        system.setDescription(request.description());
        if (request.environment() != null) {
            system.setEnvironment(request.environment());
        }
        if (request.enabled() != null) {
            system.setEnabled(request.enabled());
        }
        if (request.criticality() != null) {
            system.setCriticality(request.criticality());
        }
        if (request.escalationPolicy() != null) {
            system.setEscalationPolicy(request.escalationPolicy());
        }
        if (request.owningTeamId() != null) {
            Team team = teamRepository.findByIdAndOrganizationId(request.owningTeamId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.owningTeamId()));
            system.setOwningTeam(team);
        } else {
            system.setOwningTeam(null);
        }
        if (request.primaryOwnerId() != null) {
            User primary = userRepository.findById(request.primaryOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.primaryOwnerId()));
            if (!primary.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Primary owner must belong to your organization");
            }
            system.setPrimaryOwner(primary);
        } else {
            system.setPrimaryOwner(null);
        }
        if (request.backupOwnerId() != null) {
            User backup = userRepository.findById(request.backupOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.backupOwnerId()));
            if (!backup.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Backup owner must belong to your organization");
            }
            system.setBackupOwner(backup);
        } else {
            system.setBackupOwner(null);
        }

        return ConnectedSystemResponse.from(systemRepository.save(system));
    }

    @Transactional
    public void deleteSystem(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        ConnectedSystem system = systemRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConnectedSystem", "id", id));
        systemRepository.delete(system);
    }

    @Transactional
    public void updateSystemStatus(UUID id, SystemStatus status, Instant monitoredAt) {
        systemRepository.findById(id).ifPresent(system -> {
            system.setStatus(status);
            system.setLastMonitoredAt(monitoredAt);
            systemRepository.save(system);
        });
    }
}
