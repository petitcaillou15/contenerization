package com.example.social.users.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String pseudo,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password) {}
