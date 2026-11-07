package com.cse3230.university.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * UT08 — Password encoding
     * Verify that a newly created password is encoded, differs from the plaintext value, and can be verified with the configured PasswordEncoder.
     */
    @Test
    void encodePassword_shouldProduceDifferentHashAndVerifyWithRawPassword() {
        // Arrange
        String rawPassword = "SecurePassword@2026!";

        // Act
        String encodedHash1 = passwordEncoder.encode(rawPassword);
        String encodedHash2 = passwordEncoder.encode(rawPassword);

        // Assert
        assertThat(encodedHash1).isNotBlank();
        assertThat(encodedHash1).isNotEqualTo(rawPassword);
        assertThat(encodedHash1).startsWith("$2a$");
        // BCrypt includes a random salt for each hash operation, ensuring identical passwords yield distinct hashes
        assertThat(encodedHash1).isNotEqualTo(encodedHash2);

        // Verification with raw password
        assertThat(passwordEncoder.matches(rawPassword, encodedHash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, encodedHash2)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword!123", encodedHash1)).isFalse();
    }
}
