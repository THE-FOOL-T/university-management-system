package com.cse3230.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login")
    public String login() { return "login"; }
    @GetMapping({"/", "/dashboard"})
    public String dashboard() { return "dashboard"; }
    @GetMapping("/access-denied")
    public String denied() { return "access-denied"; }
}
