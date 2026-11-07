package com.cse3230.university.controller;

import com.cse3230.university.dto.*;
import com.cse3230.university.exception.ConflictException;
import com.cse3230.university.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/students")
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service = service; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("profiles", service.findAll());
        return "students/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new StudentRequest(null, null, null, null, null, null));
        return "students/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var profile = service.findById(id);
        model.addAttribute("form", new StudentRequest(profile.name(), profile.roll(), profile.department(), profile.semester(),
                profile.account().email(), null));
        model.addAttribute("id", id);
        return "students/form";
    }

    @PostMapping
    public String create(@Validated({Default.class, Create.class}) @ModelAttribute("form") StudentRequest form,
                         BindingResult binding, RedirectAttributes flash) {
        if (binding.hasErrors()) { return "students/form"; }
        try { service.create(form); }
        catch (ConflictException | DataIntegrityViolationException ex) {
            binding.reject("conflict", "Email or resource identifier is already in use.");
            return "students/form";
        }
        flash.addFlashAttribute("success", "Student created.");
        return "redirect:/students";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") StudentRequest form,
                         BindingResult binding, Model model, RedirectAttributes flash) {
        model.addAttribute("id", id);
        if (binding.hasErrors()) { return "students/form"; }
        try { service.update(id, form); }
        catch (ConflictException | DataIntegrityViolationException ex) {
            binding.reject("conflict", "Email or resource identifier is already in use.");
            return "students/form";
        }
        flash.addFlashAttribute("success", "Student updated. If you changed your own credentials, sign in again.");
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        service.delete(id);
        flash.addFlashAttribute("success", "Student and linked login account deleted.");
        return "redirect:/students";
    }
}
