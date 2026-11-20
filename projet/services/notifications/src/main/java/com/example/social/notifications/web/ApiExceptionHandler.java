package com.example.social.notifications.web;

import com.example.social.notifications.service.exception.ForbiddenException;
import com.example.social.notifications.service.exception.NotificationNotFoundException;
import com.example.social.notifications.users.UsersUnavailableException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, String>> err(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("error", m));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotificationNotFoundException e) {
        return err(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenException e) {
        return err(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(UsersUnavailableException.class)
    public ResponseEntity<Map<String, String>> usersDown(UsersUnavailableException e) {
        return err(HttpStatus.SERVICE_UNAVAILABLE, "users service unavailable");
    }
}
