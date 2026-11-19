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

        String code = String.format("%06d", random.nextInt(900000) + 100000);
        user.setOtpCode(code);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        return ResponseEntity.ok(m(
            "status", "SUCCESS", 
            "message", "Verification code generated successfully.",
            "debugCode", code
        ));
    }

    @PostMapping("/request-sms")
    public ResponseEntity requestSmsOtp(@RequestBody Map<String, String> payload) {
        String phone = payload.getOrDefault("phone", "").trim();
        UserAccount user = userRepository.findByPhoneNumber(phone).orElse(null);

        if (user == null) {
            // For demo convenience, if phone doesn't match an existing record exactly, fallback to any user or create code
            user = userRepository.findAll().stream().findFirst().orElse(null);
            if (user == null) {
                return ResponseEntity.ok(m("message", "If that phone number is registered, an SMS code has been sent."));
            }
        }

        String code = String.format("%06d", random.nextInt(900000) + 100000);
        user.setOtpCode(code);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        System.out.println(">>> [SMS TEXT OTP SENT to " + phone + "]: " + code);

        return ResponseEntity.ok(m(
            "status", "SUCCESS", 
            "message", "SMS verification code dispatched.",
            "debugCode", code
        ));
    }

  @PostMapping("/verify")
public ResponseEntity verifyOtp(@RequestBody Map<String, String> payload, HttpServletRequest request) {
    String rawEmail = payload.getOrDefault("email", "").trim().toLowerCase();
    String rawPhone = payload.getOrDefault("phone", "").trim();
    String code = payload.getOrDefault("code", "").trim();

    UserAccount user = null;
    if (!rawEmail.isEmpty()) {
        user = userRepository.findByEmail(rawEmail).orElse(null);
    }
    if (user == null && !rawPhone.isEmpty()) {
        user = userRepository.findByPhoneNumber(rawPhone).orElse(null);
    }

    if (user == null || user.getOtpCode() == null || user.getOtpExpiry() == null) {
        return ResponseEntity.badRequest().body(m("message", "Invalid or expired code."));
    }

    if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
        return ResponseEntity.badRequest().body(m("message", "Verification code has expired."));
    }

    if (!user.getOtpCode().equals(code)) {
        return ResponseEntity.badRequest().body(m("message", "Incorrect verification code."));
    }

    user.setOtpCode(null);
    user.setOtpExpiry(null);
    userRepository.save(user);

    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
    );

    SecurityContextHolder.getContext().setAuthentication(authToken);
    HttpSession session = request.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

    boolean isManager = "ROLE_MANAGER".equals(user.getRole());
    return ResponseEntity.ok(m("status", "SUCCESS", "redirect", isManager ? "/index.html" : "/employee.html"));
}
}