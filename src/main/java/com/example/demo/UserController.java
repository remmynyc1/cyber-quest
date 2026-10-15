package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.demo.Dtos.m;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_EMPLOYEE", "ROLE_ANALYST", "ROLE_MANAGER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Dtos.UserView> getAllUsers() {
        return userRepository.findAll().stream().map(Dtos.UserView::of).toList();
    }

    /** Creates an account with a one-time temporary password, shown once to the manager. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody Dtos.CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (!ALLOWED_ROLES.contains(req.role())) {
            return ResponseEntity.badRequest().body(m("message", "Unknown role."));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(m("message", "That email is already registered."));
        }
        String temp = Dtos.tempPassword();
        UserAccount u = new UserAccount();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setRole(req.role());
        u.setPasswordHash(passwordEncoder.encode(temp));
        u.setEnrolledInStarterQuest(true);
        u.setMustChangePassword(true);
        u = userRepository.save(u);
        return ResponseEntity.ok(m("id", u.getId(), "email", u.getEmail(), "tempPassword", temp));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            String temp = Dtos.tempPassword();
            u.setPasswordHash(passwordEncoder.encode(temp));
            u.setMustChangePassword(true);
            u.setFailedLoginAttempts(0);   // a reset also unlocks the account
            u.setLockedUntil(null);
            userRepository.save(u);
            return ResponseEntity.ok(m("email", u.getEmail(), "tempPassword", temp));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        return userRepository.findById(id).map(u -> {
            if (u.getEmail().equalsIgnoreCase(auth.getName())) {
                return ResponseEntity.status(409).<Void>build();   // cannot delete yourself
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
