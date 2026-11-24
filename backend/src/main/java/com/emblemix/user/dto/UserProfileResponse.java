package com.emblemix.user.dto;

import com.emblemix.user.Role;
import com.emblemix.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID organizationId,
        String organizationName,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getOrganization().getName(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
