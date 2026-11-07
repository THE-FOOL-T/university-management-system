package com.cse3230.university.integration;

import com.cse3230.university.dto.AdminRequest;
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
class StudentRbacIT {

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
     * IT03 — Student RBAC restrictions
     * Verifies that STUDENT can access Student APIs, but is strictly FORBIDDEN from Teacher and Admin APIs.
     * Uses valid CSRF tokens on write operations to ensure 403 Forbidden is driven by RBAC.
     */
    @Test
    @WithMockUser(username = "student@university.test", roles = {"STUDENT"})
    void studentRbac_shouldAllowStudentEndpointsAndForbidAdminAndTeacher() throws Exception {
        // 1. Permitted: Students can read Student collection endpoint
        mvc.perform(get("/api/students"))
                .andExpect(status().isOk());

        // 2. Permitted write: Students can create Student records (assignment scope)
        StudentRequest studentReq = new StudentRequest("Peer Student", "S-401", "CSE", 2, "peer.student@university.test", "Password123!");
        mvc.perform(post("/api/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(studentReq)))
                .andExpect(status().isCreated());

        // 3. Forbidden: Students cannot read Teacher endpoints
        mvc.perform(get("/api/teachers"))
                .andExpect(status().isForbidden());

        // 4. Forbidden write: Students cannot create Teacher records
        TeacherRequest teacherReq = new TeacherRequest("Target Teacher", "T-401", "CSE", "Lecturer", "target.t@university.test", "Password123!");
        mvc.perform(post("/api/teachers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(teacherReq)))
                .andExpect(status().isForbidden());

        // 5. Forbidden: Students cannot read Admin endpoints
        mvc.perform(get("/api/admins"))
                .andExpect(status().isForbidden());

        // 6. Forbidden write: Students cannot create Admin records
        AdminRequest adminReq = new AdminRequest("Target Admin", "Dean", "target.a@university.test", "Password123!");
        mvc.perform(post("/api/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(adminReq)))
                .andExpect(status().isForbidden());
    }
}
