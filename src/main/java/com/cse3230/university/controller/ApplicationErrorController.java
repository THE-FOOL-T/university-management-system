package com.cse3230.university.controller;

import com.cse3230.university.exception.ApiError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/** Safe fallback for errors raised before a REST controller can be selected. */
@Controller
public class ApplicationErrorController implements ErrorController {
    @RequestMapping("/error")
    public Object error(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = statusAttribute instanceof Integer code ? HttpStatus.resolve(code) : null;
        if (status == null) { status = HttpStatus.INTERNAL_SERVER_ERROR; }
        String originalPath = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (originalPath == null) { originalPath = request.getRequestURI(); }
        String message = switch (status) {
            case NOT_FOUND -> "The requested resource was not found";
            case METHOD_NOT_ALLOWED -> "HTTP method is not supported for this resource";
            case UNSUPPORTED_MEDIA_TYPE -> "The request content type is not supported";
            case FORBIDDEN -> "Access denied or CSRF token missing";
            case BAD_REQUEST -> "The request could not be understood";
            default -> "The request could not be completed";
        };
        if (originalPath.startsWith(request.getContextPath() + "/api/")) {
            return ResponseEntity.status(status).body(ApiError.of(status, message, originalPath, Map.of()));
        }
        ModelAndView page = new ModelAndView(status == HttpStatus.FORBIDDEN ? "error/403" : "error");
        page.setStatus(status);
        page.addObject("status", status.value());
        page.addObject("safeMessage", message);
        return page;
    }
}
