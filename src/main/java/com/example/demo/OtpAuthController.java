package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

import static com.example.demo.Dtos.m;

@RestController
@RequestMapping("/api/auth/otp")
public class OtpAuthController {

    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final Random random = new Random();

    public OtpAuthController(UserRepository userRepository, CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

@PostMapping("/request")
    public ResponseEntity requestOtp(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").trim().toLowerCase();
        UserAccount user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(m("message", "If that email is registered, a verification code has been sent."));
        }

        // Generate a 6-digit code
        String code = String.format("%06d", random.nextInt(900000) + 100000);
        user.setOtpCode(code);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5)); // Valid for 5 minutes
        userRepository.save(user);

        // Console log fallback
        System.out.println(">>> [CYBER QUEST OTP] Verification code for " + email + ": " + code);

        // Return the code in JSON so the frontend can display it for easy testing
        return ResponseEntity.ok(m(
            "status", "SUCCESS", 
            "message", "Verification code generated successfully.",
            "debugCode", code // <-- This allows your frontend to show the code instantly
        ));
    }

    // 2. Verify code and log in
    @PostMapping("/verify")
    public ResponseEntity verifyOtp(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String email = payload.getOrDefault("email", "").trim().toLowerCase();
        String code = payload.getOrDefault("code", "").trim();

        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getOtpCode() == null || user.getOtpExpiry() == null) {
            return ResponseEntity.badRequest().body(m("message", "Invalid or expired code."));
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return ResponseEntity.badRequest().body(m("message", "Verification code has expired. Please request a new one."));
        }

        if (!user.getOtpCode().equals(code)) {
            return ResponseEntity.badRequest().body(m("message", "Incorrect verification code."));
        }

        // Clear OTP after successful use
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Establish Spring Security Session programmatically
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        boolean isManager = user.getRole().equals("ROLE_MANAGER");
        return ResponseEntity.ok(m("status", "SUCCESS", "redirect", isManager ? "/index.html" : "/employee.html"));
    }
}