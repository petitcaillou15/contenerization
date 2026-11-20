package com.example.social.users.auth;

/** Identité extraite d'un JWT validé. */
public record AuthenticatedUser(long id, String pseudo) {}
