package com.cse3230.university.service;

import com.cse3230.university.dto.AdminRequest;
import com.cse3230.university.dto.AdminResponse;
import com.cse3230.university.entity.Admin;
import com.cse3230.university.entity.Role;
import com.cse3230.university.entity.UserAccount;
import com.cse3230.university.exception.ConflictException;
import com.cse3230.university.repository.AdminRepository;
import com.cse3230.university.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RequestValidator requestValidator;

    @Mock
    private UserAccountRepository accountRepository;

    @InjectMocks
    private AdminService adminService;

    private UserAccount adminAccount;
    private Admin adminProfile;

    @BeforeEach
    void setUp() {
        adminAccount = new UserAccount("admin@university.edu", "encodedPasswordHash", Role.ADMIN);
        ReflectionTestUtils.setField(adminAccount, "id", 30L);

        adminProfile = new Admin("System Admin", "Chief Registrar", adminAccount);
        ReflectionTestUtils.setField(adminProfile, "id", 3L);
    }

    /**
     * UT06 — Admin creation
     * Verify that creating an administrator assigns ADMIN and correctly creates its related records.
     */
    @Test
    void createAdmin_shouldAssignAdminRoleAndCreateRelatedRecords() {
        // Arrange
        AdminRequest request = new AdminRequest("System Admin", "Chief Registrar", "admin@university.edu", "Password123!");
        when(accountService.create("admin@university.edu", "Password123!", Role.ADMIN)).thenReturn(adminAccount);
        when(adminRepository.saveAndFlush(any(Admin.class))).thenAnswer(invocation -> {
            Admin saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        // Act
        AdminResponse response = adminService.create(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("System Admin");
        assertThat(response.designation()).isEqualTo("Chief Registrar");
        assertThat(response.account().role()).isEqualTo(Role.ADMIN);

        verify(requestValidator).validate(request, true);
        verify(accountService).create("admin@university.edu", "Password123!", Role.ADMIN);
        verify(adminRepository).saveAndFlush(any(Admin.class));
    }

    /**
     * UT07 — Prevent deletion of final Admin
     * Verify that deleting the final active administrator is rejected, preventing administrative lockout.
     */
    @Test
    void deleteAdmin_shouldRejectDeletingLastAdministrator() {
        // Arrange: only 1 enabled administrator exists in the system
        when(adminRepository.findById(3L)).thenReturn(Optional.of(adminProfile));
        when(accountRepository.lockAccountsByRole(Role.ADMIN)).thenReturn(List.of(adminAccount));

        // Act & Assert
        assertThatThrownBy(() -> adminService.delete(3L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("last enabled administrator");

        verify(adminRepository, never()).delete(any());
        verifyNoInteractions(accountService);
    }
}
