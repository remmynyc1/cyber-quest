package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.example.demo.Dtos.m;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication auth) {
        return users.findByEmail(auth.getName())
            .map(u -> ResponseEntity.ok(m("email", u.getEmail(), "mustChangePassword", u.isMustChangePassword())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> change(@RequestBody ChangePasswordRequest req, Authentication auth) {
        UserAccount u = users.findByEmail(auth.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(404).body(m("message", "User not found."));

        String current = req.currentPassword() == null ? "" : req.currentPassword();
        String proposed = req.newPassword() == null ? "" : req.newPassword();
        if (!encoder.matches(current, u.getPasswordHash())) {
            return ResponseEntity.badRequest().body(m("message", "Your current password is incorrect."));
        }
        String problem = policyProblem(proposed, current, u.getEmail());
        if (problem != null) return ResponseEntity.badRequest().body(m("message", problem));

        u.setPasswordHash(encoder.encode(proposed));
        u.setMustChangePassword(false);
        users.save(u);
        return ResponseEntity.ok(m("message", "Password updated."));
    }

    /** Returns a human-readable problem, or null when the password is acceptable. */
    static String policyProblem(String pw, String current, String email) {
        if (pw.length() < 12) return "Use at least 12 characters.";
        if (pw.getBytes(StandardCharsets.UTF_8).length > 72) return "That password is too long (72 bytes maximum).";
        if (!pw.matches(".*[A-Za-z].*") || !pw.matches(".*\\d.*")) return "Include at least one letter and one number.";
        if (pw.equals(current)) return "Choose a password different from your current one.";
        String local = email == null ? "" : email.split("@")[0].toLowerCase();
        if (local.length() >= 3 && pw.toLowerCase().contains(local)) return "Do not include the first part of your email in your password.";
        return null;
    }
}
