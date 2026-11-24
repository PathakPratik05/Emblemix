package com.emblemix.alert;

import com.emblemix.alert.dto.AlertRuleRequest;
import com.emblemix.alert.dto.AlertRuleResponse;
import com.emblemix.common.response.ApiResponse;
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
@RequestMapping("/api/v1/alert-rules")
@Tag(name = "Alert Rules", description = "Alert Rule Management Engine")
public class AlertRuleController {

    private final AlertRuleService ruleService;

    public AlertRuleController(AlertRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Create a new configurable alert rule")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(@Valid @RequestBody AlertRuleRequest request) {
        AlertRuleResponse response = ruleService.createRule(request);
        return new ResponseEntity<>(ApiResponse.success("Alert rule created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all alert rules for the current organization")
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getRules() {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getRules()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert rule by ID")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getRuleById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Update an existing alert rule")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Alert rule updated successfully",
                ruleService.updateRule(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Delete an alert rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Alert rule deleted successfully", null));
    }
}
