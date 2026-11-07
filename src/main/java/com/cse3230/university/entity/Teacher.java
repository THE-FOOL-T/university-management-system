package com.cse3230.university.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 40, unique = true)
    private String employeeId;
    @Column(nullable = false, length = 100)
    private String department;
    @Column(nullable = false, length = 100)
    private String designation;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    protected Teacher() { }

    public Teacher(String name, String employeeId, String department, String designation, UserAccount account) {
        this.name = name;
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.account = account;
        validateRole();
    }

    @PrePersist @PreUpdate
    private void validateRole() {
        if (account == null || account.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Teacher requires a TEACHER account");
        }
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
