package com.emblemix.team.dto;

import com.emblemix.team.Team;
import com.emblemix.team.TeamType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record TeamResponse(
        UUID id,
        UUID organizationId,
        String name,
        TeamType teamType,
        String description,
        UUID leadId,
        String leadName,
        List<TeamMemberResponse> members,
        int memberCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static TeamResponse from(Team team) {
        List<TeamMemberResponse> memberResponses = team.getMembers() != null
                ? team.getMembers().stream().map(TeamMemberResponse::from).collect(Collectors.toList())
                : List.of();

        String leadName = team.getLead() != null
                ? team.getLead().getFirstName() + " " + team.getLead().getLastName()
                : null;
        UUID leadId = team.getLead() != null ? team.getLead().getId() : null;

        return new TeamResponse(
                team.getId(),
                team.getOrganization().getId(),
                team.getName(),
                team.getTeamType(),
                team.getDescription(),
                leadId,
                leadName,
                memberResponses,
                memberResponses.size(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
