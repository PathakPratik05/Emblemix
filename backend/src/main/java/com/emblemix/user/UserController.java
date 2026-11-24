package com.emblemix.user;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.user.dto.UpdateProfileRequest;
import com.emblemix.user.dto.UserProfileResponse;
import com.emblemix.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User Profile and Org Members Management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                userService.updateCurrentUserProfile(request)));
    }

    @GetMapping
    @Operation(summary = "List users within current organization")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getOrganizationUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getOrganizationUsers()));
    }
}
