package com.emblemix.team.dto;

import com.emblemix.team.TeamMember;
import com.emblemix.team.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        TeamRole role,
        Instant joinedAt
) {
    public static TeamMemberResponse from(TeamMember member) {
        var user = member.getUser();
        return new TeamMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
