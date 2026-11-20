package com.example.social.users.service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("invalid email or password"); }
}
