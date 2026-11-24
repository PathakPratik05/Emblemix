package com.emblemix.team;

import com.emblemix.audit.AuditService;
import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.team.dto.*;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       AuditService auditService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        return teamRepository.findByOrganizationId(orgId).stream()
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        if (teamRepository.existsByOrganizationIdAndName(orgId, request.name().trim())) {
            throw new BadRequestException("Team with name '" + request.name() + "' already exists in this organization");
        }

        User lead = null;
        if (request.leadId() != null) {
            lead = userRepository.findById(request.leadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.leadId()));
            if (!lead.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Team lead must belong to your organization");
            }
        }

        Team team = new Team(
                UUID.randomUUID(),
                organization,
                request.name().trim(),
                request.teamType(),
                request.description() != null ? request.description().trim() : null,
                lead
        );

        Team saved = teamRepository.save(team);

        // If lead was specified, add lead as team member automatically
        if (lead != null) {
            TeamMember leadMember = new TeamMember(saved, lead, TeamRole.LEAD);
            teamMemberRepository.save(leadMember);
            saved.getMembers().add(leadMember);
        }

        User currentUser = getCurrentUser();
        auditService.log(organization, currentUser, "CREATE_TEAM", "TEAM", saved.getId().toString(),
                "Created team: " + saved.getName() + " (" + saved.getTeamType() + ")");

        log.info("Created team {} in organization {}", saved.getName(), orgId);
        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        if (teamRepository.existsByOrganizationIdAndNameAndIdNot(orgId, request.name().trim(), teamId)) {
            throw new BadRequestException("Another team with name '" + request.name() + "' already exists");
        }

        User lead = null;
        if (request.leadId() != null) {
            lead = userRepository.findById(request.leadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.leadId()));
            if (!lead.getOrganization().getId().equals(orgId)) {
                throw new BadRequestException("Team lead must belong to your organization");
            }
        }

        team.setName(request.name().trim());
        team.setTeamType(request.teamType());
        team.setDescription(request.description() != null ? request.description().trim() : null);
        team.setLead(lead);

        if (lead != null && !teamMemberRepository.existsByTeamIdAndUserId(teamId, lead.getId())) {
            TeamMember leadMember = new TeamMember(team, lead, TeamRole.LEAD);
            teamMemberRepository.save(leadMember);
            team.getMembers().add(leadMember);
        }

        Team saved = teamRepository.save(team);
        User currentUser = getCurrentUser();
        auditService.log(team.getOrganization(), currentUser, "UPDATE_TEAM", "TEAM", saved.getId().toString(),
                "Updated team: " + saved.getName());

        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse addMember(UUID teamId, AddTeamMemberRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));

        if (!user.getOrganization().getId().equals(orgId)) {
            throw new BadRequestException("User does not belong to your organization");
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new BadRequestException("User is already a member of team '" + team.getName() + "'");
        }

        TeamMember member = new TeamMember(team, user, request.role() != null ? request.role() : TeamRole.MEMBER);
        teamMemberRepository.save(member);
        team.getMembers().add(member);

        User currentUser = getCurrentUser();
        auditService.log(team.getOrganization(), currentUser, "ADD_TEAM_MEMBER", "TEAM", team.getId().toString(),
                "Added user " + user.getEmail() + " as " + member.getRole());

        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse removeMember(UUID teamId, UUID userId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BadRequestException("User is not a member of this team");
        }

        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
        team.getMembers().removeIf(m -> m.getUser().getId().equals(userId));

        if (team.getLead() != null && team.getLead().getId().equals(userId)) {
            team.setLead(null);
            teamRepository.save(team);
        }

        User currentUser = getCurrentUser();
        auditService.log(team.getOrganization(), currentUser, "REMOVE_TEAM_MEMBER", "TEAM", team.getId().toString(),
                "Removed user " + userId + " from team " + team.getName());

        return TeamResponse.from(team);
    }

    @Transactional
    public void deleteTeam(UUID teamId) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Team team = teamRepository.findByIdAndOrganizationId(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User currentUser = getCurrentUser();
        auditService.log(team.getOrganization(), currentUser, "DELETE_TEAM", "TEAM", team.getId().toString(),
                "Deleted team: " + team.getName());

        teamRepository.delete(team);
        log.info("Deleted team {} from organization {}", teamId, orgId);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(UUID userId) {
        return teamMemberRepository.findByUserId(userId).stream()
                .map(m -> TeamResponse.from(m.getTeam()))
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUserPrincipal()
                .flatMap(p -> userRepository.findById(p.getId()))
                .orElse(null);
    }
}
