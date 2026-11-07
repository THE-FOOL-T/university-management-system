package com.cse3230.university.service;

import com.cse3230.university.entity.*;
import com.cse3230.university.exception.ConflictException;
import com.cse3230.university.repository.UserAccountRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AccountService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;

    public AccountService(UserAccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    public UserAccount create(String email, String password, Role role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmail(normalized)) { throw new ConflictException("Email is already in use"); }
        return accounts.save(new UserAccount(normalized, passwords.encode(password), role));
    }

    public void update(UserAccount account, String email, String password) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailAndIdNot(normalized, account.getId())) {
            throw new ConflictException("Email is already in use");
        }
        account.setEmail(normalized);
        if (password != null && !password.isEmpty()) { account.setPasswordHash(passwords.encode(password)); }
    }

    public void delete(UserAccount account) { accounts.delete(account); }
}
