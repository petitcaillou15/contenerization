package com.example.social.posts.events;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

/**
 * Événement publié sur Redis quand un commentaire est créé.
 * posts ignore qui consomme cet événement (découplage fort, spec §1.1).
 * Clés JSON en snake_case : contrat partagé avec le service notifications.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentCreatedEvent(
        long postId,
        long postAuthorId,
        long commentAuthorId,
        String commentAuthorPseudo,
        long commentId,
        Instant createdAt) {}
