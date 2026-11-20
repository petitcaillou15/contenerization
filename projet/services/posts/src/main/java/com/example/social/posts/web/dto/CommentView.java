package com.example.social.posts.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.social.posts.domain.Comment;
import java.time.Instant;

public record CommentView(long id,
                          @JsonProperty("author_id") long authorId,
                          String content,
                          @JsonProperty("created_at") Instant createdAt) {
    public static CommentView of(Comment c) {
        return new CommentView(c.getId(), c.getAuthorId(), c.getContent(), c.getCreatedAt());
    }
}
