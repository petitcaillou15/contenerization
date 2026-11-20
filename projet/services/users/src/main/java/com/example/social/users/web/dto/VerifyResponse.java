package com.example.social.users.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Contrat CONSOMMÉ par posts et notifications. Ne pas renommer les champs. */
public record VerifyResponse(
        boolean valid,
        @JsonProperty("user_id") long userId,
        String pseudo) {}
