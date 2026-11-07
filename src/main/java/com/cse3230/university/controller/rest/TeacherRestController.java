package com.cse3230.university.controller.rest;

import com.cse3230.university.dto.*;
import com.cse3230.university.service.TeacherService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
public class TeacherRestController {
    private final TeacherService service;
    public TeacherRestController(TeacherService service) { this.service = service; }

    @GetMapping
    public List<TeacherResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public TeacherResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/teachers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
