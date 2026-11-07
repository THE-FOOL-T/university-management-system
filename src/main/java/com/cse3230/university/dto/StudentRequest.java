package com.cse3230.university.dto;

import jakarta.validation.constraints.*;

public record StudentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String roll,
        @NotBlank @Size(max = 100) String department,
        @NotNull @Min(1) @Max(12) Integer semester,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank(groups = Create.class) @ValidPassword String password) {
    public StudentRequest {
        if (email != null) { email = email.trim().toLowerCase(java.util.Locale.ROOT); }
    }

    // Never include a raw credential in a diagnostic string.
    @Override public String toString() { return "StudentRequest[redacted]"; }
}
