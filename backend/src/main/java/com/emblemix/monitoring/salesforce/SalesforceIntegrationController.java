package com.emblemix.monitoring.salesforce;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.monitoring.salesforce.dto.*;
import com.emblemix.monitoring.salesforce.service.SalesforceMonitoringService;
import com.emblemix.monitoring.salesforce.service.SalesforceOAuthService;
import com.emblemix.system.ConnectedSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/salesforce")
@Tag(name = "Salesforce Integration", description = "Salesforce OAuth 2.0 and Org Monitoring")
public class SalesforceIntegrationController {

    private final SalesforceOAuthService oauthService;
    private final SalesforceMonitoringService monitoringService;

    public SalesforceIntegrationController(SalesforceOAuthService oauthService,
                                           SalesforceMonitoringService monitoringService) {
        this.oauthService = oauthService;
        this.monitoringService = monitoringService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Register a new Salesforce system for monitoring")
    public ResponseEntity<ApiResponse<SalesforceConfigResponse>> createSalesforceSystem(
            @Valid @RequestBody SalesforceConfigRequest request) {
        SalesforceConfigResponse response = monitoringService.createSalesforceSystem(request);
        return new ResponseEntity<>(ApiResponse.success("Salesforce system registered", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all registered Salesforce configurations")
    public ResponseEntity<ApiResponse<List<SalesforceConfigResponse>>> getConfigurations() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getConfigurations()));
    }

    @GetMapping("/authorize")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Generate Salesforce OAuth 2.0 Web Server authorization URL")
    public ResponseEntity<ApiResponse<SalesforceAuthUrlResponse>> getAuthorizationUrl(
            @RequestParam UUID systemId,
            @RequestParam(defaultValue = "false") boolean isSandbox) {
        return ResponseEntity.ok(ApiResponse.success(oauthService.generateAuthorizationUrl(systemId, isSandbox)));
    }

    @GetMapping("/callback")
    @Operation(summary = "Salesforce OAuth 2.0 callback endpoint")
    public void handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response
    ) throws IOException {
        ConnectedSystem system = oauthService.handleCallback(code, state);
        // Redirect back to frontend salesforce integrations page with success query
        response.sendRedirect("/integrations/salesforce?connected=true&systemId=" + system.getId());
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Execute an on-demand test check of Salesforce API limits and Apex jobs")
    public ResponseEntity<ApiResponse<SalesforceTestResultDto>> testConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.testSalesforceConnection(id)));
    }

    @PostMapping("/{id}/disconnect")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Disconnect Salesforce integration and securely revoke credentials")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable UUID id) {
        oauthService.disconnect(id);
        return ResponseEntity.ok(ApiResponse.success("Salesforce integration disconnected successfully", null));
    }
}
