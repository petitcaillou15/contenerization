package com.example.social.users.service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long id) { super("user " + id + " not found"); }
}
