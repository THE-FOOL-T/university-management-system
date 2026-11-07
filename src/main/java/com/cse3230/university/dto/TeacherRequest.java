package com.cse3230.university.dto;

import jakarta.validation.constraints.*;

public record TeacherRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String employeeId,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 100) String designation,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank(groups = Create.class) @ValidPassword String password) {
    public TeacherRequest {
        if (email != null) { email = email.trim().toLowerCase(java.util.Locale.ROOT); }
    }

    // Never include a raw credential in a diagnostic string.
    @Override public String toString() { return "TeacherRequest[redacted]"; }
}
