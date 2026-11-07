package com.cse3230.university.repository;

import com.cse3230.university.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRoll(String value);
    boolean existsByRollAndIdNot(String value, Long id);
}
