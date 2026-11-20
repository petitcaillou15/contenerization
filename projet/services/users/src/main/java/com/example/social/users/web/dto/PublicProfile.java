package com.example.social.users.web.dto;

import com.example.social.users.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record PublicProfile(long id, String pseudo, String bio,
                            @JsonProperty("created_at") Instant createdAt) {
    public static PublicProfile of(User u) {
        return new PublicProfile(u.getId(), u.getPseudo(), u.getBio(), u.getCreatedAt());
    }
}
