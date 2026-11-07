package com.cse3230.university.dto;

import com.cse3230.university.entity.Student;

public record StudentResponse(Long id, String name, String roll, String department, Integer semester, AccountResponse account) {
    public static StudentResponse from(Student profile) {
        return new StudentResponse(profile.getId(), profile.getName(), profile.getRoll(), profile.getDepartment(), profile.getSemester(),
                AccountResponse.from(profile.getAccount()));
    }
}
