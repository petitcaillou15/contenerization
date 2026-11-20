package com.example.social.posts.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.social.posts.domain.Post;
import java.time.Instant;

public record PostSummary(long id, String title,
                          @JsonProperty("author_id") long authorId,
                          @JsonProperty("created_at") Instant createdAt) {
    public static PostSummary of(Post p) {
        return new PostSummary(p.getId(), p.getTitle(), p.getAuthorId(), p.getCreatedAt());
    }
}
