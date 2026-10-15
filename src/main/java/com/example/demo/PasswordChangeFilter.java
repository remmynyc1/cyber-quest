package com.example.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Users with a temporary password can only reach the change-password page until they set a new one. */
public final class PasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of(
        "/change-password.html", "/api/account/password", "/api/account/status",
        "/common.js", "/logout", "/favicon.ico", "/error");

    private final UserRepository users;

    public PasswordChangeFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && !ALLOWED.contains(request.getRequestURI())
                && users.findByEmail(auth.getName()).map(UserAccount::isMustChangePassword).orElse(false)) {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"PASSWORD_CHANGE_REQUIRED\"}");
            } else {
                response.sendRedirect("/change-password.html");
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
