package com.cse3230.university.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admins")
public class Admin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 100)
    private String designation;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    protected Admin() { }

    public Admin(String name, String designation, UserAccount account) {
        this.name = name;
        this.designation = designation;
        this.account = account;
        validateRole();
    }

    @PrePersist @PreUpdate
    private void validateRole() {
        if (account == null || account.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Admin requires a ADMIN account");
        }
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
