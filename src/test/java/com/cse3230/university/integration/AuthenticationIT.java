package com.cse3230.university.integration;

import com.cse3230.university.entity.Role;
import com.cse3230.university.entity.UserAccount;
import com.cse3230.university.repository.AdminRepository;
import com.cse3230.university.repository.StudentRepository;
import com.cse3230.university.repository.TeacherRepository;
import com.cse3230.university.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIT {

    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository accountRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    /**
     * IT04 — Authentication and Logout
     * Verifies full authentication lifecycle: credentials verification, session establishment,
     * protected resource access, CSRF-protected logout, and session invalidation.
     */
    @Test
    void authenticationAndLogout_shouldEstablishSessionAndInvalidateOnLogout() throws Exception {
        // 1. Seed a known test user in the PostgreSQL database
        String rawPassword = "ValidPassword123!";
        accountRepository.saveAndFlush(
                new UserAccount("auth.test@university.test", encoder.encode(rawPassword), Role.ADMIN));

        // 2. Test invalid credentials -> rejected with redirect to /login?error
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "auth.test@university.test")
                        .param("password", "IncorrectPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // 3. Test valid credentials -> successful login redirecting to /dashboard with active session
        MvcResult loginResult = mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "auth.test@university.test")
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 4. Verify authenticated session can access protected resource /api/auth/me
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auth.test@university.test"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // 5. Logout using CSRF token and the active session
        mvc.perform(post("/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // 6. Verify former session is invalidated and cannot access protected resource
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
