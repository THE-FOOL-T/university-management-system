package com.cse3230.university.security;

import com.cse3230.university.entity.UserAccount;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AccountPrincipal implements UserDetails {
    @Serial private static final long serialVersionUID = 1L;
    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean enabled;

    public AccountPrincipal(UserAccount account) {
        id = account.getId();
        email = account.getEmail();
        passwordHash = account.getPasswordHash();
        role = account.getRole().name();
        enabled = account.isEnabled();
    }
    public Long getId() { return id; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
