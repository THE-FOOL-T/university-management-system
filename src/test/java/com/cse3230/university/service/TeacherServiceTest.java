package com.cse3230.university.service;

import com.cse3230.university.dto.TeacherRequest;
import com.cse3230.university.dto.TeacherResponse;
import com.cse3230.university.entity.Role;
import com.cse3230.university.entity.Teacher;
import com.cse3230.university.entity.UserAccount;
import com.cse3230.university.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RequestValidator requestValidator;

    @InjectMocks
    private TeacherService teacherService;

    private UserAccount teacherAccount;

    @BeforeEach
    void setUp() {
        teacherAccount = new UserAccount("teacher@university.edu", "encodedPasswordHash", Role.TEACHER);
        ReflectionTestUtils.setField(teacherAccount, "id", 20L);
    }

    /**
     * UT05 — Teacher creation
     * Verify that creating a teacher assigns TEACHER role and persists the correct teacher details.
     */
    @Test
    void createTeacher_shouldAssignTeacherRoleAndPersistDetails() {
        // Arrange
        TeacherRequest request = new TeacherRequest("Dr. Robert Smith", "EMP-202", "CSE", "Associate Professor", "teacher@university.edu", "Password123!");
        when(teacherRepository.existsByEmployeeId("EMP-202")).thenReturn(false);
        when(accountService.create("teacher@university.edu", "Password123!", Role.TEACHER)).thenReturn(teacherAccount);
        when(teacherRepository.saveAndFlush(any(Teacher.class))).thenAnswer(invocation -> {
            Teacher saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        // Act
        TeacherResponse response = teacherService.create(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Dr. Robert Smith");
        assertThat(response.employeeId()).isEqualTo("EMP-202");
        assertThat(response.department()).isEqualTo("CSE");
        assertThat(response.designation()).isEqualTo("Associate Professor");
        assertThat(response.account().role()).isEqualTo(Role.TEACHER);

        verify(requestValidator).validate(request, true);
        verify(teacherRepository).existsByEmployeeId("EMP-202");
        verify(accountService).create("teacher@university.edu", "Password123!", Role.TEACHER);
        verify(teacherRepository).saveAndFlush(any(Teacher.class));
    }
}
