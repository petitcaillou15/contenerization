package com.example.social.posts.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record PostDetail(long id, String title, String content,
                         @JsonProperty("author_id") long authorId,
                         @JsonProperty("created_at") Instant createdAt,
                         List<CommentView> comments) {}
