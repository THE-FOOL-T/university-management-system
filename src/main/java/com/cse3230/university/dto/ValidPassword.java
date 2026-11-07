package com.cse3230.university.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    String message() default "must contain at least 10 characters and at most 72 UTF-8 bytes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
