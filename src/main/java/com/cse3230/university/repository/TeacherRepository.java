package com.cse3230.university.repository;

import com.cse3230.university.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    boolean existsByEmployeeId(String value);
    boolean existsByEmployeeIdAndIdNot(String value, Long id);
}
