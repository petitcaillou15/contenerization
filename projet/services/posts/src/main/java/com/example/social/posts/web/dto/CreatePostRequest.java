package com.example.social.posts.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 5000) String content) {}
