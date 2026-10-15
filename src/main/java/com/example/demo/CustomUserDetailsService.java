package com.example.demo;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByEmail(email == null ? "" : email.trim().toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String roleName = account.getRole().replace("ROLE_", "");

        return User.builder()
            .username(account.getEmail())
            .password(account.getPasswordHash())
            .roles(roleName)
            .accountLocked(account.getLockedUntil() != null
                && java.time.LocalDateTime.now().isBefore(account.getLockedUntil()))
            .build();
    }
}
