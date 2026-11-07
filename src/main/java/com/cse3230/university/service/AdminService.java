package com.cse3230.university.service;

import com.cse3230.university.dto.*;
import com.cse3230.university.entity.*;
import com.cse3230.university.exception.*;
import com.cse3230.university.repository.*;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Transactional(readOnly = true)
public class AdminService {
    private final AdminRepository profiles;
    private final AccountService accounts;
    private final RequestValidator validator;
    private final UserAccountRepository accountRepository;

    public AdminService(AdminRepository profiles, AccountService accounts, RequestValidator validator, UserAccountRepository accountRepository) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.validator = validator;
        this.accountRepository = accountRepository;
    }

    public List<AdminResponse> findAll() {
        return profiles.findAll(Sort.by("id")).stream().map(AdminResponse::from).toList();
    }

    public AdminResponse findById(Long id) { return AdminResponse.from(requireProfile(id)); }

    @Transactional
    public AdminResponse create(AdminRequest request) {
        validator.validate(request, true);
        UserAccount account = accounts.create(request.email(), request.password(), Role.ADMIN);
        Admin profile = new Admin(request.name().trim(), request.designation().trim(), account);
        // Flush here: database uniqueness races fail inside this transaction.
        return AdminResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public AdminResponse update(Long id, AdminRequest request) {
        validator.validate(request, false);
        Admin profile = requireProfile(id);
        accounts.update(profile.getAccount(), request.email(), request.password());
        profile.setName(request.name().trim());
        profile.setDesignation(request.designation().trim());
        return AdminResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long id) {
        var admins = accountRepository.lockAccountsByRole(Role.ADMIN);
        Admin profile = requireProfile(id);
        if (profile.getAccount().isEnabled() && admins.stream().filter(UserAccount::isEnabled).count() <= 1) {
            throw new ConflictException("The last enabled administrator cannot be deleted");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName().equals(profile.getAccount().getEmail())) {
            throw new ConflictException("You cannot delete your own administrator account");
        }
        UserAccount account = profile.getAccount();
        profiles.delete(profile);
        profiles.flush(); // Remove the referencing row before its account (foreign key).
        accounts.delete(account);
    }

    private Admin requireProfile(Long id) {
        return profiles.findById(id).orElseThrow(() -> new NotFoundException("Admin " + id + " was not found"));
    }
}
