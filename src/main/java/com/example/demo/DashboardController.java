package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;

    public DashboardController(UserRepository userRepository, SecurityLogRepository securityLogRepository) {
        this.userRepository = userRepository;
        this.securityLogRepository = securityLogRepository;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        long totalUsers = userRepository.count();
        long activeAlerts = securityLogRepository.countByResolvedFalse();

        return Map.of(
            "totalOperatives", totalUsers,
            "activeThreats", activeAlerts,
            "systemStatus", activeAlerts > 0 ? "ELEVATED" : "OPTIMAL"
        );
    }

    @GetMapping("/logs")
    public List<SecurityLog> getRecentLogs() {
        return securityLogRepository.findAll();
    }

    @GetMapping("/leaderboard")
    public List<Dtos.ManagerLeaderEntry> leaderboard() {
        return userRepository.findAll().stream()
            .filter(u -> "ROLE_EMPLOYEE".equals(u.getRole()) || "ROLE_ANALYST".equals(u.getRole()))
            .sorted((a, b) -> Integer.compare(b.getTotalXp(), a.getTotalXp()))
            .map(u -> new Dtos.ManagerLeaderEntry(u.getFullName(), u.getEmail(), u.getTotalXp(),
                    u.getCompletedChallengeIds().size()))
            .toList();
    }

    @PostMapping("/threats/{id}/resolve")
    public ResponseEntity<SecurityLog> resolveThreat(@PathVariable Long id) {
        return securityLogRepository.findById(id).map(log -> {
            log.setResolved(true);
            return ResponseEntity.ok(securityLogRepository.save(log));
        }).orElse(ResponseEntity.notFound().build());
    }
}
