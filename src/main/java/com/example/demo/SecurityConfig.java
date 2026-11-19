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
                .ignoringRequestMatchers("/login", "/api/users/register","/api/users/demo-signup","/api/auth/otp/**","/logout"))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
       .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
    + "img-src 'self' data:; connect-src 'self' ws: wss:; frame-ancestors 'self'; form-action 'self'")))
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                        org.springframework.http.HttpStatus.UNAUTHORIZED),
                request -> request.getRequestURI().startsWith("/api/")))
         .authorizeHttpRequests(auth -> auth
    .requestMatchers("/login.html", "/login", "/error", "/favicon.ico", "/common.js", "/api/users/register", "/api/users/demo-signup", "/api/auth/otp/**", "/css/**", "/js/**").permitAll()
    .requestMatchers("/api/quest/terminal-auth").authenticated() // <-- Must be authenticated
    .requestMatchers("/index.html", "/training.html", "/api/users/**", "/api/dashboard/**", "/api/training/**", "/api/threats/**").hasRole("MANAGER")
    .requestMatchers("/employee.html", "/api/quest/**", "/api/sim/**").hasAnyRole("EMPLOYEE", "ANALYST", "MANAGER")
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
            .rememberMe(remember -> remember
                .key("cyberQuestSecureKey2026")
                .tokenValiditySeconds(1209600)
        )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            );
  
            return http.build();
    }
}