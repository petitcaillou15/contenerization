package com.example.social.notifications.users;

/** users injoignable (connexion, timeout, 5xx). Mène à un 503 sur toute route authentifiée. */
public class UsersUnavailableException extends RuntimeException {
    public UsersUnavailableException(String message, Throwable cause) { super(message, cause); }
    public UsersUnavailableException(String message) { super(message); }
}
