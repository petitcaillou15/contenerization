package com.example.social.posts.service.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() { super("not the author"); }
}
