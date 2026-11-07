package com.cse3230.university.integration;

import com.cse3230.university.dto.StudentRequest;
import com.cse3230.university.repository.AdminRepository;
import com.cse3230.university.repository.StudentRepository;
import com.cse3230.university.repository.TeacherRepository;
import com.cse3230.university.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCrudIT {

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
     * IT01 — Admin completes Student CRUD
     * Exercises the full Controller -> Service -> Repository -> PostgreSQL cycle for Student resources.
     */
    @Test
    @WithMockUser(username = "admin@university.edu", roles = {"ADMIN"})
    void adminShouldCompleteFullStudentCrudCycle() throws Exception {
        // 1. Authenticate as an administrator & Create Student through POST /api/students
        StudentRequest createReq = new StudentRequest("David Clark", "STU-1001", "CSE", 1, "david.clark@university.test", "Password123!");
        MvcResult postResult = mvc.perform(post("/api/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("David Clark"))
                .andExpect(jsonPath("$.roll").value("STU-1001"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.semester").value(1))
                .andExpect(jsonPath("$.account.role").value("STUDENT"))
                .andReturn();

        JsonNode createdJson = mapper.readTree(postResult.getResponse().getContentAsString());
        long studentId = createdJson.get("id").asLong();

        // 2. Read newly created Student through GET /api/students/{id}
        mvc.perform(get("/api/students/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.name").value("David Clark"))
                .andExpect(jsonPath("$.roll").value("STU-1001"));

        // 3. Update Student through PUT /api/students/{id}
        StudentRequest updateReq = new StudentRequest("David Clark Updated", "STU-1001", "CSE", 2, "david.clark@university.test", "");
        mvc.perform(put("/api/students/{id}", studentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.name").value("David Clark Updated"))
                .andExpect(jsonPath("$.semester").value(2));

        // 4. Delete Student through DELETE /api/students/{id}
        mvc.perform(delete("/api/students/{id}", studentId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // 5. Verify Student and linked Account are deleted from PostgreSQL
        assertThat(studentRepository.findById(studentId)).isEmpty();
        assertThat(accountRepository.findByEmail("david.clark@university.test")).isEmpty();
    }
}
