package com.cse3230.university.dto;

import com.cse3230.university.entity.Teacher;

public record TeacherResponse(Long id, String name, String employeeId, String department, String designation, AccountResponse account) {
    public static TeacherResponse from(Teacher profile) {
        return new TeacherResponse(profile.getId(), profile.getName(), profile.getEmployeeId(), profile.getDepartment(), profile.getDesignation(),
                AccountResponse.from(profile.getAccount()));
    }
}
