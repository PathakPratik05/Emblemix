package com.emblemix.common.security;

import com.emblemix.common.exception.UnauthorizedException;
import com.emblemix.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal getRequiredCurrentUser() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public static UUID getRequiredCurrentOrgId() {
        UUID tenant = TenantContext.getCurrentTenant();
        if (tenant != null) {
            return tenant;
        }
        return getRequiredCurrentUser().getOrganizationId();
    }

    public static boolean hasRole(Role role) {
        return getCurrentUserPrincipal()
                .map(p -> p.getRole() == role || p.getRole() == Role.SUPER_ADMIN)
                .orElse(false);
    }
}
