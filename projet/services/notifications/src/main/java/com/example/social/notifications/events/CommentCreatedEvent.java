package com.example.social.notifications.events;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

/** Événement reçu de posts via Redis. Même forme que le DTO publié par posts. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentCreatedEvent(
        long postId,
        long postAuthorId,
        long commentAuthorId,
        String commentAuthorPseudo,
        long commentId,
        Instant createdAt) {}
