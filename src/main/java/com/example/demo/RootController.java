package com.example.demo;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class RootController {

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
            return isManager ? "redirect:/index.html" : "redirect:/employee.html";
        }
        return "redirect:/login.html";
    }

    @GetMapping(value = {"/login", "/login.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> loginPage() {
        return loadHtml("static/login.html");
    }

    @GetMapping(value = {"/index", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> indexPage() {
        return loadHtml("static/index.html");
    }

    @GetMapping(value = {"/employee", "/employee.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> employeePage() {
        return loadHtml("static/employee.html");
    }

    private ResponseEntity<String> loadHtml(String resourcePath) {
        try {
            Resource res = new ClassPathResource(resourcePath);
            if (res.exists()) {
                return ResponseEntity.ok(new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {}
        return ResponseEntity.status(404).body("<h2>Resource not found: " + resourcePath + "</h2>");
    }
}
