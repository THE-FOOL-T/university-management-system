package com.cse3230.university.config;

import com.cse3230.university.dto.AdminRequest;
import com.cse3230.university.entity.Role;
import com.cse3230.university.repository.UserAccountRepository;
import com.cse3230.university.service.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapAdmin implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final AdminService admins;
    private final String name;
    private final String email;
    private final String password;

    public BootstrapAdmin(UserAccountRepository accounts, AdminService admins,
                          @Value("${app.bootstrap.name:}") String name,
                          @Value("${app.bootstrap.email:}") String email,
                          @Value("${app.bootstrap.password:}") String password) {
        this.accounts = accounts;
        this.admins = admins;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.existsByRoleAndEnabledTrue(Role.ADMIN)) { return; }
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("First startup requires BOOTSTRAP_ADMIN_NAME, "
                    + "BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD");
        }
        // Uses the same validation, BCrypt hashing and transaction as ordinary creation.
        admins.create(new AdminRequest(name, "System administrator", email, password));
    }
}
