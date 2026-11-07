package com.cse3230.university.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Empty means keep the current hash on PUT. @NotBlank requires it on POST.
        return value == null || value.isEmpty()
                || (!value.isBlank() && value.codePointCount(0, value.length()) >= 10
                && value.getBytes(StandardCharsets.UTF_8).length <= 72);
    }
}
