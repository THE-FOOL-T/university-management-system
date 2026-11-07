package com.cse3230.university.service;

import com.cse3230.university.dto.StudentRequest;
import com.cse3230.university.dto.StudentResponse;
import com.cse3230.university.entity.Role;
import com.cse3230.university.entity.Student;
import com.cse3230.university.entity.UserAccount;
import com.cse3230.university.exception.ConflictException;
import com.cse3230.university.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RequestValidator requestValidator;

    @InjectMocks
    private StudentService studentService;

    private UserAccount testAccount;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testAccount = new UserAccount("student@university.edu", "encodedPasswordHash", Role.STUDENT);
        ReflectionTestUtils.setField(testAccount, "id", 10L);

        testStudent = new Student("Alice Walker", "S101", "CSE", 3, testAccount);
        ReflectionTestUtils.setField(testStudent, "id", 1L);
    }

    /**
     * UT01 — Student creation
     * Verify that creating a student stores the correct information, assigns the STUDENT role, and uses password encoding.
     */
    @Test
    void createStudent_shouldAssignStudentRoleAndEncodePassword() {
        // Arrange
        StudentRequest request = new StudentRequest("Alice Walker", "S101", "CSE", 3, "student@university.edu", "Password123!");
        when(studentRepository.existsByRoll("S101")).thenReturn(false);
        when(accountService.create("student@university.edu", "Password123!", Role.STUDENT)).thenReturn(testAccount);
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // Act
        StudentResponse response = studentService.create(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alice Walker");
        assertThat(response.roll()).isEqualTo("S101");
        assertThat(response.department()).isEqualTo("CSE");
        assertThat(response.semester()).isEqualTo(3);
        assertThat(response.account().role()).isEqualTo(Role.STUDENT);

        verify(requestValidator).validate(request, true);
        verify(studentRepository).existsByRoll("S101");
        verify(accountService).create("student@university.edu", "Password123!", Role.STUDENT);
        verify(studentRepository).saveAndFlush(any(Student.class));
    }

    /**
     * UT02 — Duplicate student email
     * Verify that an existing email causes a meaningful conflict/validation exception and no duplicate account is saved.
     */
    @Test
    void createStudent_shouldRejectDuplicateEmail() {
        // Arrange
        StudentRequest request = new StudentRequest("Bob Smith", "S102", "CSE", 1, "taken@university.edu", "Password123!");
        when(studentRepository.existsByRoll("S102")).thenReturn(false);
        when(accountService.create("taken@university.edu", "Password123!", Role.STUDENT))
                .thenThrow(new ConflictException("Email is already in use"));

        // Act & Assert
        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already in use");

        verify(requestValidator).validate(request, true);
        verify(accountService).create("taken@university.edu", "Password123!", Role.STUDENT);
        verify(studentRepository, never()).saveAndFlush(any());
    }

    /**
     * UT03 — Student update
     * Verify that updating a student modifies the permitted fields and preserves the correct identity and role.
     */
    @Test
    void updateStudent_shouldModifyPermittedFieldsAndPreserveRole() {
        // Arrange
        StudentRequest updateRequest = new StudentRequest("Alice Updated", "S101", "EEE", 4, "alice.new@university.edu", "NewPassword123!");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.existsByRollAndIdNot("S101", 1L)).thenReturn(false);
        when(studentRepository.saveAndFlush(testStudent)).thenReturn(testStudent);

        // Act
        StudentResponse response = studentService.update(1L, updateRequest);

        // Assert
        assertThat(response.name()).isEqualTo("Alice Updated");
        assertThat(response.department()).isEqualTo("EEE");
        assertThat(response.semester()).isEqualTo(4);
        assertThat(testStudent.getAccount().getRole()).isEqualTo(Role.STUDENT);

        verify(requestValidator).validate(updateRequest, false);
        verify(accountService).update(testAccount, "alice.new@university.edu", "NewPassword123!");
        verify(studentRepository).saveAndFlush(testStudent);
    }

    /**
     * UT04 — Student deletion
     * Verify that deleting a student invokes the expected persistence operations and handles the linked account according to the implemented deletion policy.
     */
    @Test
    void deleteStudent_shouldInvokeExpectedPersistenceAndAccountDeletion() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // Act
        studentService.delete(1L);

        // Assert
        InOrder inOrder = inOrder(studentRepository, accountService);
        inOrder.verify(studentRepository).delete(testStudent);
        inOrder.verify(studentRepository).flush();
        inOrder.verify(accountService).delete(testAccount);
    }
}
