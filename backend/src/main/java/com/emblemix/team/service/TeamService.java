package com.emblemix.team.service;

import com.emblemix.team.dto.AddTeamMemberRequest;
import com.emblemix.team.dto.CreateTeamRequest;
import com.emblemix.team.dto.TeamResponse;
import com.emblemix.team.dto.UpdateTeamRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for multi-disciplinary organization teams, leads, members, and permissions.
 */
public interface TeamService {

    /**
     * Lists all teams registered in the current tenant organization.
     */
    List<TeamResponse> getTeams();

    /**
     * Retrieves team details and active member roster by team ID.
     */
    TeamResponse getTeamById(UUID teamId);

    /**
     * Creates a new engineering team (e.g., DEVOPS, SALESFORCE, WEB_ENGINEERING) with an optional lead.
     */
    TeamResponse createTeam(CreateTeamRequest request);

    /**
     * Updates an existing team's name, type, description, or lead.
     */
    TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request);

    /**
     * Adds an organization user to a team with a designated team role (LEAD or MEMBER).
     */
    TeamResponse addMember(UUID teamId, AddTeamMemberRequest request);

    /**
     * Removes an engineer from a team roster.
     */
    TeamResponse removeMember(UUID teamId, UUID userId);

    /**
     * Deletes a team and unassigns members and system ownership.
     */
    void deleteTeam(UUID teamId);

    /**
     * Lists teams to which a specific user belongs.
     */
    List<TeamResponse> getUserTeams(UUID userId);
}
