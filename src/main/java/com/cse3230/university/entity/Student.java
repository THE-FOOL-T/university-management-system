package com.cse3230.university.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 40, unique = true)
    private String roll;
    @Column(nullable = false, length = 100)
    private String department;
    @Column(nullable = false)
    private Integer semester;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    protected Student() { }

    public Student(String name, String roll, String department, Integer semester, UserAccount account) {
        this.name = name;
        this.roll = roll;
        this.department = department;
        this.semester = semester;
        this.account = account;
        validateRole();
    }

    @PrePersist @PreUpdate
    private void validateRole() {
        if (account == null || account.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Student requires a STUDENT account");
        }
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoll() { return roll; }
    public void setRoll(String roll) { this.roll = roll; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
}
