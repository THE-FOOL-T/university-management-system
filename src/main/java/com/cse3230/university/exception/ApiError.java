package com.cse3230.university.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, Map<String, String> fieldErrors) {
    public static ApiError of(HttpStatus status, String message, String path, Map<String, String> fields) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fields);
    }
}
