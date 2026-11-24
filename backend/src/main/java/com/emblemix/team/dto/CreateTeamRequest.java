package com.emblemix.team.dto;

import com.emblemix.team.TeamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTeamRequest(
        @NotBlank(message = "Team name is required")
        String name,

        @NotNull(message = "Team type is required")
        TeamType teamType,

        String description,

        UUID leadId
) {}
