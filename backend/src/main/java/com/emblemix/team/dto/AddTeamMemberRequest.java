package com.emblemix.team.dto;

import com.emblemix.team.TeamRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddTeamMemberRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        TeamRole role
) {}
