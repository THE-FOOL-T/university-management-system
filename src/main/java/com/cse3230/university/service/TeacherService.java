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
public class TeacherService {
    private final TeacherRepository profiles;
    private final AccountService accounts;
    private final RequestValidator validator;

    public TeacherService(TeacherRepository profiles, AccountService accounts, RequestValidator validator) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.validator = validator;
    }

    public List<TeacherResponse> findAll() {
        return profiles.findAll(Sort.by("id")).stream().map(TeacherResponse::from).toList();
    }

    public TeacherResponse findById(Long id) { return TeacherResponse.from(requireProfile(id)); }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        validator.validate(request, true);
        if (profiles.existsByEmployeeId(request.employeeId().trim())) {
            throw new ConflictException("employeeId is already in use");
        }
        UserAccount account = accounts.create(request.email(), request.password(), Role.TEACHER);
        Teacher profile = new Teacher(request.name().trim(), request.employeeId().trim(), request.department().trim(), request.designation().trim(), account);
        // Flush here: database uniqueness races fail inside this transaction.
        return TeacherResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        validator.validate(request, false);
        Teacher profile = requireProfile(id);
        if (profiles.existsByEmployeeIdAndIdNot(request.employeeId().trim(), id)) {
            throw new ConflictException("employeeId is already in use");
        }
        accounts.update(profile.getAccount(), request.email(), request.password());
        profile.setName(request.name().trim());
        profile.setEmployeeId(request.employeeId().trim());
        profile.setDepartment(request.department().trim());
        profile.setDesignation(request.designation().trim());
        return TeacherResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long id) {
        Teacher profile = requireProfile(id);
        UserAccount account = profile.getAccount();
        profiles.delete(profile);
        profiles.flush(); // Remove the referencing row before its account (foreign key).
        accounts.delete(account);
    }

    private Teacher requireProfile(Long id) {
        return profiles.findById(id).orElseThrow(() -> new NotFoundException("Teacher " + id + " was not found"));
    }
}
