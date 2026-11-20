package com.example.social.notifications.service.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(long id) { super("notification " + id + " not found"); }
}
