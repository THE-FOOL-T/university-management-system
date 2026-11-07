package com.cse3230.university.security;

import com.cse3230.university.repository.UserAccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;
    public DatabaseUserDetailsService(UserAccountRepository accounts) { this.accounts = accounts; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return accounts.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
