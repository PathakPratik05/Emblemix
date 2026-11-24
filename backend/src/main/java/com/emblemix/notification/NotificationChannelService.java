package com.emblemix.notification;

import com.emblemix.common.exception.ResourceNotFoundException;
import com.emblemix.common.response.PagedResponse;
import com.emblemix.common.security.SecurityUtils;
import com.emblemix.notification.dto.NotificationChannelRequest;
import com.emblemix.notification.dto.NotificationChannelResponse;
import com.emblemix.notification.dto.NotificationLogResponse;
import com.emblemix.organization.Organization;
import com.emblemix.organization.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationChannelService {

    private final NotificationChannelRepository channelRepository;
    private final NotificationLogRepository logRepository;
    private final OrganizationRepository organizationRepository;

    public NotificationChannelService(NotificationChannelRepository channelRepository,
                                      NotificationLogRepository logRepository,
                                      OrganizationRepository organizationRepository) {
        this.channelRepository = channelRepository;
        this.logRepository = logRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public NotificationChannelResponse createChannel(NotificationChannelRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        NotificationChannel channel = new NotificationChannel(
                UUID.randomUUID(),
                org,
                request.channelType(),
                request.destination().trim(),
                request.enabled() != null ? request.enabled() : true,
                request.notifyOnCritical() != null ? request.notifyOnCritical() : true,
                request.notifyOnHigh() != null ? request.notifyOnHigh() : true,
                request.notifyOnResolve() != null ? request.notifyOnResolve() : true,
                request.notifyOnAcknowledge() != null ? request.notifyOnAcknowledge() : false
        );

        return NotificationChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional(readOnly = true)
    public List<NotificationChannelResponse> getChannels() {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        return channelRepository.findByOrganizationId(orgId).stream()
                .map(NotificationChannelResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationChannelResponse updateChannel(UUID id, NotificationChannelRequest request) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        NotificationChannel channel = channelRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationChannel", "id", id));

        channel.setChannelType(request.channelType());
        channel.setDestination(request.destination().trim());
        if (request.enabled() != null) channel.setEnabled(request.enabled());
        if (request.notifyOnCritical() != null) channel.setNotifyOnCritical(request.notifyOnCritical());
        if (request.notifyOnHigh() != null) channel.setNotifyOnHigh(request.notifyOnHigh());
        if (request.notifyOnResolve() != null) channel.setNotifyOnResolve(request.notifyOnResolve());
        if (request.notifyOnAcknowledge() != null) channel.setNotifyOnAcknowledge(request.notifyOnAcknowledge());

        return NotificationChannelResponse.from(channelRepository.save(channel));
    }

    @Transactional
    public void deleteChannel(UUID id) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        NotificationChannel channel = channelRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationChannel", "id", id));
        channelRepository.delete(channel);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationLogResponse> getNotificationLogs(Pageable pageable) {
        UUID orgId = SecurityUtils.getRequiredCurrentOrgId();
        Page<NotificationLog> page = logRepository.findByOrganizationId(orgId, pageable);
        return new PagedResponse<>(
                page.map(NotificationLogResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
