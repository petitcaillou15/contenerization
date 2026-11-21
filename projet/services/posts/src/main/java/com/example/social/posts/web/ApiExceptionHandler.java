package com.example.social.posts.web;

import com.example.social.posts.service.exception.ContentTooLongException;
import com.example.social.posts.service.exception.ForbiddenException;
import com.example.social.posts.service.exception.PostNotFoundException;
import com.example.social.posts.service.exception.WritesDisabledException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, String>> err(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("error", m));
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(PostNotFoundException e) {
        return err(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ContentTooLongException.class)
    public ResponseEntity<Map<String, String>> tooLong(ContentTooLongException e) {
        return err(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenException e) {
        return err(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(WritesDisabledException.class)
    public ResponseEntity<Map<String, String>> writesOff(WritesDisabledException e) {
        return err(HttpStatus.SERVICE_UNAVAILABLE, "writes temporarily disabled");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("invalid request");
        return err(HttpStatus.BAD_REQUEST, msg);
    }
}
