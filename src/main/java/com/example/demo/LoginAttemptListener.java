package com.example.demo;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Locks an account for 15 minutes after 5 consecutive failed sign-ins and records the event for managers. */
@Component
public class LoginAttemptListener {

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final SecurityLogRepository logs;

    public LoginAttemptListener(UserRepository users, SecurityLogRepository logs) {
        this.users = users;
        this.logs = logs;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String name = event.getAuthentication().getName();
        if (name == null) return;
        users.findByEmail(name.trim().toLowerCase()).ifPresent(u -> {
            int attempts = u.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILURES) {
                u.setFailedLoginAttempts(0);
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                SecurityLog log = new SecurityLog();
                log.setEventType("ACCOUNT_LOCKED");
                log.setSeverity("MEDIUM");
                log.setDescription("Account locked for " + LOCK_MINUTES + " minutes after " + MAX_FAILURES + " failed sign-ins.");
                log.setTargetEmail(u.getEmail());
                log.setResolved(false);
                logs.save(log);
            } else {
                u.setFailedLoginAttempts(attempts);
            }
            users.save(u);
        });
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        users.findByEmail(event.getAuthentication().getName()).ifPresent(u -> {
            if (u.getFailedLoginAttempts() != 0 || u.getLockedUntil() != null) {
                u.setFailedLoginAttempts(0);
                u.setLockedUntil(null);
                users.save(u);
            }
        });
    }
}
