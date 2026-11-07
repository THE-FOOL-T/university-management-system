package com.cse3230.university.controller;

import com.cse3230.university.dto.*;
import com.cse3230.university.exception.ConflictException;
import com.cse3230.university.service.TeacherService;
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
@RequestMapping("/teachers")
public class TeacherController {
    private final TeacherService service;
    public TeacherController(TeacherService service) { this.service = service; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("profiles", service.findAll());
        return "teachers/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new TeacherRequest(null, null, null, null, null, null));
        return "teachers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var profile = service.findById(id);
        model.addAttribute("form", new TeacherRequest(profile.name(), profile.employeeId(), profile.department(), profile.designation(),
                profile.account().email(), null));
        model.addAttribute("id", id);
        return "teachers/form";
    }

    @PostMapping
    public String create(@Validated({Default.class, Create.class}) @ModelAttribute("form") TeacherRequest form,
                         BindingResult binding, RedirectAttributes flash) {
        if (binding.hasErrors()) { return "teachers/form"; }
        try { service.create(form); }
        catch (ConflictException | DataIntegrityViolationException ex) {
            binding.reject("conflict", "Email or resource identifier is already in use.");
            return "teachers/form";
        }
        flash.addFlashAttribute("success", "Teacher created.");
        return "redirect:/teachers";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") TeacherRequest form,
                         BindingResult binding, Model model, RedirectAttributes flash) {
        model.addAttribute("id", id);
        if (binding.hasErrors()) { return "teachers/form"; }
        try { service.update(id, form); }
        catch (ConflictException | DataIntegrityViolationException ex) {
            binding.reject("conflict", "Email or resource identifier is already in use.");
            return "teachers/form";
        }
        flash.addFlashAttribute("success", "Teacher updated. If you changed your own credentials, sign in again.");
        return "redirect:/teachers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        service.delete(id);
        flash.addFlashAttribute("success", "Teacher and linked login account deleted.");
        return "redirect:/teachers";
    }
}
