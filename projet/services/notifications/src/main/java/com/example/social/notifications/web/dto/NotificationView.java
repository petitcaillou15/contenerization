package com.example.social.notifications.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.social.notifications.domain.Notification;
import java.time.Instant;

public record NotificationView(long id, String type, JsonNode payload, boolean read,
                               @JsonProperty("created_at") Instant createdAt) {

    public static NotificationView of(Notification n, ObjectMapper mapper) {
        JsonNode payload;
        try {
            payload = mapper.readTree(n.getPayload());
        } catch (Exception e) {
            payload = mapper.createObjectNode(); // payload illisible -> objet vide plutôt que planter
        }
        return new NotificationView(n.getId(), n.getType(), payload, n.isRead(), n.getCreatedAt());
    }
}
