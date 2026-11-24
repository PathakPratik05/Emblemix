package com.emblemix.team;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.team.dto.*;
import com.emblemix.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Organization-level team collaboration and ownership management")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @Operation(summary = "List all teams in current organization")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeams() {
        return ResponseEntity.ok(ApiResponse.success("Teams retrieved successfully", teamService.getTeams()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team details by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Team retrieved successfully", teamService.getTeamById(id)));
    }

    @GetMapping("/my-teams")
    @Operation(summary = "Get teams the authenticated user belongs to")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyTeams() {
        UUID currentUserId = SecurityUtils.getRequiredCurrentUser().getId();
        return ResponseEntity.ok(ApiResponse.success("User teams retrieved", teamService.getUserTeams(currentUserId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Create a new organization team")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse created = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Update an existing team")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse updated = teamService.updateTeam(id, request);
        return ResponseEntity.ok(ApiResponse.success("Team updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Operation(summary = "Delete a team")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(ApiResponse.success("Team deleted successfully", null));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ENGINEER')")
    @Operation(summary = "Add a member to a team")
    public ResponseEntity<ApiResponse<TeamResponse>> addMember(@PathVariable UUID id,
                                                               @Valid @RequestBody AddTeamMemberRequest request) {
        TeamResponse updated = teamService.addMember(id, request);
        return ResponseEntity.ok(ApiResponse.success("Member added to team successfully", updated));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ENGINEER')")
    @Operation(summary = "Remove a member from a team")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMember(@PathVariable UUID id,
                                                                  @PathVariable UUID userId) {
        TeamResponse updated = teamService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed from team successfully", updated));
    }
}
