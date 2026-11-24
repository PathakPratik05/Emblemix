package com.emblemix.organization;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.organization.dto.OrganizationResponse;
import com.emblemix.organization.dto.UpdateOrganizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Tenant Organization Management")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current authenticated tenant organization details")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getCurrentOrganization() {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getCurrentOrganization()));
    }

    @PutMapping("/current")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update current tenant organization details (Org Admins only)")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateCurrentOrganization(
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully",
                organizationService.updateCurrentOrganization(request)));
    }
}
