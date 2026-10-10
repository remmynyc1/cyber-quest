package com.example.demo;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicFirewallFilter implements Filter {

    // Thread-safe collection of blocked attacker IP addresses
    private static final Set<String> BLOCKED_IPS = ConcurrentHashMap.newKeySet();

    public static void blockIp(String ipAddress) {
        BLOCKED_IPS.add(ipAddress.trim());
    }

    public static Set<String> getBlockedIps() {
        return BLOCKED_IPS;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        if (BLOCKED_IPS.contains(clientIp)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getWriter().write("CYBER QUEST ACTIVE MITIGATION: IP blocked by pfSense integration rule.");
            return;
        }

        chain.doFilter(request, response);
    }
}