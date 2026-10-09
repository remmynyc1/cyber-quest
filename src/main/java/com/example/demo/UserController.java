package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserAccount> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public UserAccount createUser(@RequestBody UserAccount newUser) {
        // Assign a default placeholder password hash if none is provided
        newUser.setPasswordHash("$2a$10$DefaultHashedPasswordPlaceholderStringForMockPurposes");
        return userRepository.save(newUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}