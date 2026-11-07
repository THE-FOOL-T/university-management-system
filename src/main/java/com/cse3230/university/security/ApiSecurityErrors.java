package com.cse3230.university.security;

import com.cse3230.university.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityErrors {
    private final ObjectMapper json;
    public ApiSecurityErrors(ObjectMapper json) { this.json = json; }

    public void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    public void denied(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken;
        if (request.getRequestURI().startsWith("/api/")) {
            if (anonymous) { unauthorized(request, response); return; }
            write(request, response, HttpStatus.FORBIDDEN, exception instanceof CsrfException
                    ? "CSRF token is missing or invalid" : "Your role cannot access this resource");
        } else {
            response.sendError(403, "Access denied or CSRF token missing");
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiError.of(status, message, request.getRequestURI(), Map.of()));
    }
}
