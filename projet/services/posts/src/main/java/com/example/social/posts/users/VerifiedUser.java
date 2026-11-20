package com.example.social.posts.users;

/** Identité renvoyée par GET /verify de users. */
public record VerifiedUser(long id, String pseudo) {}
