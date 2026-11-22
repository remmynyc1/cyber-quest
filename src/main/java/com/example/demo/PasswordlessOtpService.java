package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordlessOtpService {

    private record OtpEntry(String code, Instant expiresAt, Instant issuedAt, int attempts) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private final UserRepository userRepository;

    @Value("${cq.mail.api-key:}")
    private String apiKey;

    @Value("${cq.mail.from-email:}")
    private String fromEmail;

    public PasswordlessOtpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String requestCode(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) return null;
        String email = rawEmail.trim().toLowerCase();

        OtpEntry existing = store.get(email);
        if (existing != null && Instant.now().isBefore(existing.issuedAt().plusSeconds(30))) {
            return email.endsWith(".internal") ? existing.code() : null;
        }

        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            return null;
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant now = Instant.now();
        store.put(email, new OtpEntry(code, now.plus(5, ChronoUnit.MINUTES), now, 0));

        boolean isInternalDemo = email.endsWith(".internal");
        if (!isInternalDemo && !apiKey.isBlank() && !fromEmail.isBlank()) {
            boolean sent = sendBrevoEmail(email, code);
            return sent ? null : code;
        } else {
            System.out.println("[CyberQuest OTP Login] Code for " + email + " -> " + code);
            return code;
        }
    }

    public boolean verifyCode(String rawEmail, String rawCode) {
        if (rawEmail == null || rawCode == null) return false;
        String email = rawEmail.trim().toLowerCase();
        String code = rawCode.trim();

        OtpEntry entry = store.get(email);
        if (entry == null) return false;

        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email);
            return false;
        }

        if (!entry.code().equals(code)) {
            int nextAttempts = entry.attempts() + 1;
            if (nextAttempts >= 3) {
                store.remove(email);
            } else {
                store.put(email, new OtpEntry(entry.code(), entry.expiresAt(), entry.issuedAt(), nextAttempts));
            }
            return false;
        }

        store.remove(email);
        return true;
    }

    private boolean sendBrevoEmail(String to, String code) {
        String text = "Your Cyber Quest one-time sign-in code is: " + code + "\n\nThis code expires in 5 minutes.";
        String body = "{\"sender\":{\"name\":\"Cyber Quest SOC\",\"email\":\"" + escapeJson(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}],"
                + "\"subject\":\"Cyber Quest // One-Time Sign-In Code\","
                + "\"textContent\":\"" + escapeJson(text) + "\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                System.err.println("[Brevo Mail Error] status=" + res.statusCode() + " body=" + res.body());
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}