package com.example.demo;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String routeUserByRole(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login.html";
        }

        boolean isManager = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_CEO"));

        if (isManager) {
            return "redirect:/index.html";
        }
        return "redirect:/employee.html";
    }
}