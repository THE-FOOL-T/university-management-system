package com.cse3230.university.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @JsonIgnore
    @Column(nullable = false, length = 60)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private Role role;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserAccount() { }

    public UserAccount(String email, String passwordHash, Role role) {
        setEmail(email);
        setPasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setEmail(String email) { this.email = email.trim().toLowerCase(Locale.ROOT); }
    public void setPasswordHash(String hash) { this.passwordHash = Objects.requireNonNull(hash); }
}
