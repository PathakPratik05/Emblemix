package com.emblemix.user.service;

import com.emblemix.user.dto.UpdateProfileRequest;
import com.emblemix.user.dto.UserProfileResponse;

import java.util.List;

/**
 * Service interface for user profile operations and organizational user queries.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * Updates the name and profile details of the currently authenticated user.
     */
    UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request);

    /**
     * Retrieves all users belonging to the current organization tenant.
     */
    List<UserProfileResponse> getOrganizationUsers();
}
