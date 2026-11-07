package com.cse3230.university.integration;

import com.cse3230.university.dto.StudentRequest;
import com.cse3230.university.dto.TeacherRequest;
import com.cse3230.university.repository.AdminRepository;
import com.cse3230.university.repository.StudentRepository;
import com.cse3230.university.repository.TeacherRepository;
import com.cse3230.university.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherRbacIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private UserAccountRepository accountRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    /**
     * IT02 — Teacher RBAC restrictions
     * Verifies that TEACHER can access Teacher APIs, but is strictly FORBIDDEN from Admin and Student APIs.
     * Uses valid CSRF tokens to ensure 403 Forbidden is caused by RBAC, not CSRF failure.
     */
    @Test
    @WithMockUser(username = "teacher@university.test", roles = {"TEACHER"})
    void teacherRbac_shouldAllowTeacherEndpointsAndForbidAdminAndStudent() throws Exception {
        // 1. Permitted: Teachers can read Teacher collection endpoint
        mvc.perform(get("/api/teachers"))
                .andExpect(status().isOk());

        // 2. Permitted write: Teachers can create Teacher records
        TeacherRequest teacherReq = new TeacherRequest("Prof. Alan Turing", "EMP-301", "CSE", "Professor", "alan.turing@university.test", "Password123!");
        mvc.perform(post("/api/teachers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(teacherReq)))
                .andExpect(status().isCreated());

        // 3. Forbidden: Teachers cannot read Admin endpoints
        mvc.perform(get("/api/admins"))
                .andExpect(status().isForbidden());

        // 4. Forbidden: Teachers cannot read Student endpoints
        mvc.perform(get("/api/students"))
                .andExpect(status().isForbidden());

        // 5. Forbidden write: Teachers cannot create Student records
        StudentRequest studentReq = new StudentRequest("Attempted Student", "S-999", "CSE", 1, "attempt@university.test", "Password123!");
        mvc.perform(post("/api/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(studentReq)))
                .andExpect(status().isForbidden());
    }
}
