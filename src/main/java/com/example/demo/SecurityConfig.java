package com.example.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Forces the XSRF-TOKEN cookie to be written so /common.js can send it back as X-XSRF-TOKEN. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
            chain.doFilter(request, response);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/login"))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data:; frame-ancestors 'none'; form-action 'self'")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/login", "/error", "/favicon.ico", "/common.js").permitAll()
                .requestMatchers("/index.html", "/training.html", "/api/users/**", "/api/dashboard/**",
                                 "/api/training/**", "/api/threats/**").hasRole("MANAGER")
                .requestMatchers("/employee.html", "/api/quest/**", "/api/sim/**")
                    .hasAnyRole("EMPLOYEE", "ANALYST", "MANAGER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    boolean isManager = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                    response.sendRedirect(isManager ? "/index.html" : "/employee.html");
                })
                .failureUrl("/login.html?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            );

        return http.build();
    }
}
