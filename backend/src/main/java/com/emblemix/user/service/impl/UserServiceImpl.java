package com.emblemix.user.service.impl;

import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.user.User;
import com.emblemix.user.UserRepository;
import com.emblemix.user.dto.UpdateProfileRequest;
import com.emblemix.user.dto.UserProfileResponse;
import com.emblemix.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        UUID userId = SecurityUtils.getRequiredCurrentUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        UUID userId = SecurityUtils.getRequiredCurrentUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getOrganizationUsers() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        return userRepository.findByOrganizationId(orgId).stream()
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
}
