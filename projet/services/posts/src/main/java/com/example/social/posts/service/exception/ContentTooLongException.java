package com.example.social.posts.service.exception;

public class ContentTooLongException extends RuntimeException {
    public ContentTooLongException(int max) {
        super("content must be at most " + max + " characters");
    }
}
