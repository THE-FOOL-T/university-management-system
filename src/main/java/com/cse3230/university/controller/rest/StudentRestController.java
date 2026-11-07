package com.cse3230.university.controller.rest;

import com.cse3230.university.dto.*;
import com.cse3230.university.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentRestController {
    private final StudentService service;
    public StudentRestController(StudentService service) { this.service = service; }

    @GetMapping
    public List<StudentResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/students/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
