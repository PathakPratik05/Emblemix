package com.emblemix.incident.dto;

import com.emblemix.incident.IncidentHistory;
import java.time.Instant;
import java.util.UUID;

public record IncidentHistoryDto(
        UUID id,
        String action,
        String fromStatus,
        String toStatus,
        String details,
        String userName,
        Instant createdAt
) {
    public static IncidentHistoryDto from(IncidentHistory history) {
        String userName = "System";
        if (history.getUser() != null) {
            userName = history.getUser().getFirstName() + " " + history.getUser().getLastName();
        }
        return new IncidentHistoryDto(
                history.getId(),
                history.getAction(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getDetails(),
                userName,
                history.getCreatedAt()
        );
    }
}
