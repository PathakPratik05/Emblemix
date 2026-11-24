package com.emblemix.team;

import com.emblemix.audit.AuditService;
import com.emblemix.common.exception.BadRequestException;
import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.TenantContext;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import com.emblemix.team.dto.*;
import com.emblemix.user.Role;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditService auditService;

    private TeamService teamService;

    private UUID orgId;
    private Organization org;
    private User lead;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                teamRepository,
                teamMemberRepository,
                userRepository,
                organizationRepository,
                auditService
        );

        orgId = UUID.randomUUID();
        TenantContext.setCurrentTenant(orgId);

        org = new Organization(orgId, "Acme Corp", "acme-corp");
        lead = new User(UUID.randomUUID(), org, "lead@acme.com", "hash", "Jane", "Lead", Role.ENGINEER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create team and automatically add lead as team member")
    void testCreateTeamWithLead() {
        CreateTeamRequest request = new CreateTeamRequest("DevOps Core", TeamType.DEVOPS, "Primary infra team", lead.getId());

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(teamRepository.existsByOrganizationIdAndName(orgId, "DevOps Core")).thenReturn(false);
        when(userRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamResponse response = teamService.createTeam(request);

        assertNotNull(response);
        assertEquals("DevOps Core", response.name());
        assertEquals(TeamType.DEVOPS, response.teamType());
        assertEquals(lead.getId(), response.leadId());
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(auditService).log(eq(org), any(), eq("CREATE_TEAM"), eq("TEAM"), any(), any());
    }

    @Test
    @DisplayName("Should reject creating team if name already exists in organization")
    void testCreateTeamDuplicateName() {
        CreateTeamRequest request = new CreateTeamRequest("DevOps Core", TeamType.DEVOPS, "Desc", null);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(teamRepository.existsByOrganizationIdAndName(orgId, "DevOps Core")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> teamService.createTeam(request));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add member to team")
    void testAddMember() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team(teamId, org, "Web Eng", TeamType.WEB_ENGINEERING, "Desc", null);
        User member = new User(UUID.randomUUID(), org, "dev@acme.com", "hash", "John", "Dev", Role.ENGINEER);

        when(teamRepository.findByIdAndOrganizationId(teamId, orgId)).thenReturn(Optional.of(team));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, member.getId())).thenReturn(false);

        AddTeamMemberRequest request = new AddTeamMemberRequest(member.getId(), TeamRole.MEMBER);
        TeamResponse response = teamService.addMember(teamId, request);

        assertNotNull(response);
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("Should remove member from team")
    void testRemoveMember() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team(teamId, org, "SecOps", TeamType.SECURITY, "Desc", null);
        UUID userId = UUID.randomUUID();

        when(teamRepository.findByIdAndOrganizationId(teamId, orgId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        TeamResponse response = teamService.removeMember(teamId, userId);

        assertNotNull(response);
        verify(teamMemberRepository).deleteByTeamIdAndUserId(teamId, userId);
    }
}
