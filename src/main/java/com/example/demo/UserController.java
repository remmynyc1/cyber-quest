package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

   public UserController(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JdbcTemplate jdbcTemplate) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
}

private void clearUserAttempts(Long userId) {
    for (String sql : List.of(
            "DELETE FROM sim_attempt WHERE user_id = ?",
            "DELETE FROM sim_attempt WHERE user_account_id = ?",
            "DELETE FROM quest_attempt WHERE user_id = ?",
            "DELETE FROM challenge_attempt WHERE user_id = ?"
    )) {
        try {
            jdbcTemplate.update(sql, userId);
        } catch (Exception ex) {
            System.out.println("Reset SQL skipped (" + sql + "): " + ex.getMessage());
        }
    }
}

    @GetMapping
    public List<Dtos.UserView> getAllUsers() {
        return userRepository.findAll().stream().map(Dtos.UserView::of).toList();
    }

    @PostMapping
    public ResponseEntity createUser(@Valid @RequestBody Dtos.CreateUserRequest req) {
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
    public ResponseEntity resetPassword(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            String temp = Dtos.tempPassword();
            u.setPasswordHash(passwordEncoder.encode(temp));
            u.setMustChangePassword(true);
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
            return ResponseEntity.ok(m("email", u.getEmail(), "tempPassword", temp));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        return userRepository.findById(id).map(u -> {
            if (u.getEmail().equalsIgnoreCase(auth.getName())) {
                return ResponseEntity.status(409).<Void>build();
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity selfRegister(@Valid @RequestBody Dtos.SelfRegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(m("message", "That email is already registered."));
        }
        
        String problem = AccountController.policyProblem(req.password(), "", email);
        if (problem != null) {
            return ResponseEntity.badRequest().body(m("message", problem));
        }

        UserAccount u = new UserAccount();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setRole("ROLE_EMPLOYEE");
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setEnrolledInStarterQuest(true);
        u.setMustChangePassword(false);
        u = userRepository.save(u);

        return ResponseEntity.ok(m("status", "SUCCESS", "message", "Account created successfully. You may now log in."));
    }

    @PostMapping("/demo-signup")
    public ResponseEntity createDemoAccount(@RequestBody Map<String, String> payload) {
        String roleType = payload.getOrDefault("role", "employee").trim().toLowerCase();
        boolean isManager = roleType.equals("manager");
        String role = isManager ? "ROLE_MANAGER" : "ROLE_EMPLOYEE";
        String prefix = isManager ? "manager-demo-" : "trainee-demo-";
        String email = prefix + System.currentTimeMillis() + "@cyberquest.internal";
        String fullName = isManager ? "Demo Supervisor" : "Demo Trainee";
        String password = "Password123!";

        UserAccount u = new UserAccount();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEnrolledInStarterQuest(true);
        u.setMustChangePassword(false);
        userRepository.save(u);

        return ResponseEntity.ok(Map.of("email", email, "password", password));
    }

    @PostMapping("/{id}/reset-progress")
    public ResponseEntity resetProgress(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            u.setTotalXp(0);
            if (u.getCompletedChallengeIds() != null) {
                u.getCompletedChallengeIds().clear();
            }
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Progress and challenges reset successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset-all-progress")
    public ResponseEntity resetAllProgress() {
        List<UserAccount> users = userRepository.findAll();
        for (UserAccount u : users) {
            u.setTotalXp(0);
            if (u.getCompletedChallengeIds() != null) {
                u.getCompletedChallengeIds().clear();
            }
        }
        userRepository.saveAll(users);
        return ResponseEntity.ok(Map.of("message", "All operative progress globally reset."));
    }
    
}
