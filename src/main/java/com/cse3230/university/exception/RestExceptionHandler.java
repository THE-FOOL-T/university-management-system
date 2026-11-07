package com.cse3230.university.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.cse3230.university.controller.rest")
public class RestExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> missing(NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> constraint(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Email, roll, employee ID or linked account conflicts with an existing record",
                request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> validation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();
        ex.getConstraintViolations().forEach(v -> fields.put(v.getPropertyPath().toString(), v.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request, unknown JSON field, or invalid ID", request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this resource", request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> media(HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send a JSON body with Content-Type: application/json",
                request, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request,
                                           Map<String, String> fields) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI(), fields));
    }
}
