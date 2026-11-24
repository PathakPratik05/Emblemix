package com.emblemix.notification;

import com.emblemix.common.response.ApiResponse;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.notification.dto.NotificationChannelRequest;
import com.emblemix.notification.dto.NotificationChannelResponse;
import com.emblemix.notification.dto.NotificationLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification Channel Configuration and Delivery Logs")
public class NotificationController {

    private final NotificationChannelService channelService;

    public NotificationController(NotificationChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping("/channels")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Register a new notification channel")
    public ResponseEntity<ApiResponse<NotificationChannelResponse>> createChannel(
            @Valid @RequestBody NotificationChannelRequest request) {
        NotificationChannelResponse response = channelService.createChannel(request);
        return new ResponseEntity<>(ApiResponse.success("Notification channel created", response), HttpStatus.CREATED);
    }

    @GetMapping("/channels")
    @Operation(summary = "List all notification channels for current organization")
    public ResponseEntity<ApiResponse<List<NotificationChannelResponse>>> getChannels() {
        return ResponseEntity.ok(ApiResponse.success(channelService.getChannels()));
    }

    @PutMapping("/channels/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Update an existing notification channel")
    public ResponseEntity<ApiResponse<NotificationChannelResponse>> updateChannel(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationChannelRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notification channel updated",
                channelService.updateChannel(id, request)));
    }

    @DeleteMapping("/channels/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('ENGINEER')")
    @Operation(summary = "Delete a notification channel")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable UUID id) {
        channelService.deleteChannel(id);
        return ResponseEntity.ok(ApiResponse.success("Notification channel deleted", null));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get historical notification delivery logs")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationLogResponse>>> getLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getNotificationLogs(pageable)));
    }
}
