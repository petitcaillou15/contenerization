package com.example.social.posts.service.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(long id) { super("post " + id + " not found"); }
}
