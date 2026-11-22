package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/otp")
public class OtpLoginController {

    private final PasswordlessOtpService otpService;
    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public OtpLoginController(PasswordlessOtpService otpService,
                              UserRepository userRepository,
                              SecurityLogRepository securityLogRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.securityLogRepository = securityLogRepository;
    }

    @PostMapping("/request")
    public ResponseEntity requestOtp(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String demoCode = otpService.requestCode(email);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "If an active account matches that email, a 6-digit code has been sent.");
        if (demoCode != null) {
            response.put("demoCode", demoCode);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity verifyOtp(@RequestBody Map<String, String> body,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String code = body.getOrDefault("code", "").trim();

        if (!otpService.verifyCode(email, code)) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Invalid or expired one-time code."
            ));
        }

        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Account unavailable."));
        }

        // Clear any prior failed password attempts on successful OTP sign-in
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Establish Spring Security session identical to password login
        var authorities = List.of(new SimpleGrantedAuthority(user.getRole()));
        var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        SecurityLog log = new SecurityLog();
        log.setEventType("OTP_LOGIN_SUCCESS");
        log.setSeverity("LOW");
        log.setDescription("Authenticated via one-time passcode.");
        log.setTargetEmail(user.getEmail());
        log.setResolved(true);
        securityLogRepository.save(log);

        String redirect;
        if (user.isMustChangePassword()) {
            redirect = "/change-password.html";
        } else if ("ROLE_MANAGER".equals(user.getRole())) {
            redirect = "/index.html";
        } else {
            redirect = "/employee.html";
        }

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "redirect", redirect
        ));
    }
}