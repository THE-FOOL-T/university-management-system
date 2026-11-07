package com.cse3230.university.service;

import jakarta.validation.*;
import jakarta.validation.groups.Default;
import org.springframework.stereotype.Component;
import com.cse3230.university.dto.Create;

@Component
public class RequestValidator {
    private final Validator validator;
    public RequestValidator(Validator validator) { this.validator = validator; }

    public void validate(Object request, boolean creating) {
        var violations = creating ? validator.validate(request, Default.class, Create.class)
                : validator.validate(request);
        if (!violations.isEmpty()) { throw new ConstraintViolationException(violations); }
    }
}
