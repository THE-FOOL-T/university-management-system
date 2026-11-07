package com.cse3230.university.service;

import com.cse3230.university.dto.*;
import com.cse3230.university.entity.*;
import com.cse3230.university.exception.*;
import com.cse3230.university.repository.*;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository profiles;
    private final AccountService accounts;
    private final RequestValidator validator;

    public StudentService(StudentRepository profiles, AccountService accounts, RequestValidator validator) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.validator = validator;
    }

    public List<StudentResponse> findAll() {
        return profiles.findAll(Sort.by("id")).stream().map(StudentResponse::from).toList();
    }

    public StudentResponse findById(Long id) { return StudentResponse.from(requireProfile(id)); }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        validator.validate(request, true);
        if (profiles.existsByRoll(request.roll().trim())) {
            throw new ConflictException("roll is already in use");
        }
        UserAccount account = accounts.create(request.email(), request.password(), Role.STUDENT);
        Student profile = new Student(request.name().trim(), request.roll().trim(), request.department().trim(), request.semester(), account);
        // Flush here: database uniqueness races fail inside this transaction.
        return StudentResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        validator.validate(request, false);
        Student profile = requireProfile(id);
        if (profiles.existsByRollAndIdNot(request.roll().trim(), id)) {
            throw new ConflictException("roll is already in use");
        }
        accounts.update(profile.getAccount(), request.email(), request.password());
        profile.setName(request.name().trim());
        profile.setRoll(request.roll().trim());
        profile.setDepartment(request.department().trim());
        profile.setSemester(request.semester());
        return StudentResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long id) {
        Student profile = requireProfile(id);
        UserAccount account = profile.getAccount();
        profiles.delete(profile);
        profiles.flush(); // Remove the referencing row before its account (foreign key).
        accounts.delete(account);
    }

    private Student requireProfile(Long id) {
        return profiles.findById(id).orElseThrow(() -> new NotFoundException("Student " + id + " was not found"));
    }
}
