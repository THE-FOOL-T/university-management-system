package com.cse3230.university.dto;

import com.cse3230.university.entity.Admin;

public record AdminResponse(Long id, String name, String designation, AccountResponse account) {
    public static AdminResponse from(Admin profile) {
        return new AdminResponse(profile.getId(), profile.getName(), profile.getDesignation(),
                AccountResponse.from(profile.getAccount()));
    }
}
