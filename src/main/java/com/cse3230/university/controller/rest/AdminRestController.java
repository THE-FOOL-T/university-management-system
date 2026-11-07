package com.cse3230.university.controller.rest;

import com.cse3230.university.dto.*;
import com.cse3230.university.service.AdminService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
public class AdminRestController {
    private final AdminService service;
    public AdminRestController(AdminService service) { this.service = service; }

    @GetMapping
    public List<AdminResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public AdminResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<AdminResponse> create(@Valid @RequestBody AdminRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/admins/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AdminResponse update(@PathVariable Long id, @Valid @RequestBody AdminRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
