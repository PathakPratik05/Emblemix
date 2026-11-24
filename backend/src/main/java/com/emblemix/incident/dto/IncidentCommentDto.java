package com.emblemix.incident.dto;

import com.emblemix.incident.IncidentComment;
import java.time.Instant;
import java.util.UUID;

public record IncidentCommentDto(
        UUID id,
        String comment,
        UUID userId,
        String userName,
        Instant createdAt
) {
    public static IncidentCommentDto from(IncidentComment comment) {
        String userName = comment.getUser().getFirstName() + " " + comment.getUser().getLastName();
        return new IncidentCommentDto(
                comment.getId(),
                comment.getComment(),
                comment.getUser().getId(),
                userName,
                comment.getCreatedAt()
        );
    }
}
