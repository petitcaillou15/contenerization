package com.example.social.users.service.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException() { super("pseudo or email already used"); }
}
