package com.example.social.posts.service.exception;

public class WritesDisabledException extends RuntimeException {
    public WritesDisabledException() { super("writes temporarily disabled"); }
}
