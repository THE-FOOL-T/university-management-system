package com.cse3230.university.dto;

import com.cse3230.university.entity.Role;
import com.cse3230.university.entity.UserAccount;
import java.time.Instant;

public record AccountResponse(Long id, String email, Role role, boolean enabled, Instant createdAt) {
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(account.getId(), account.getEmail(), account.getRole(),
                account.isEnabled(), account.getCreatedAt());
    }
}
