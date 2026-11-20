package com.example.social.notifications.service.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() { super("not the recipient"); }
}
