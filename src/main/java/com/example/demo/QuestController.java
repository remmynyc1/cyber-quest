package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quest")
public class QuestController {

    private final UserRepository userRepository;
    private final QuestChallengeRepository challengeRepository;
    private final SecurityLogRepository securityLogRepository;

    public QuestController(UserRepository userRepository, 
                           QuestChallengeRepository challengeRepository,
                           SecurityLogRepository securityLogRepository) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.securityLogRepository = securityLogRepository;
    }

    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> getMyStats(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findByEmail(authentication.getName())
            .map(u -> ResponseEntity.ok(Map.<String, Object>of(
                "fullName", u.getFullName(),
                "email", u.getEmail(),
                "totalXp", u.getTotalXp(),
                "completedChallenges", u.getCompletedChallengeIds()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/challenges")
    public List<QuestChallenge> getAvailableChallenges() {
        return challengeRepository.findAll();
    }

    @PostMapping("/challenges/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitSolution(
            @PathVariable Long id,
            @RequestBody Map<String, String> submission,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        UserAccount user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        if (user.getCompletedChallengeIds().contains(id)) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ALREADY_SOLVED",
                "message", "Artifact already verified. XP previously awarded."
            ));
        }

        String submittedAnswer = submission.getOrDefault("answer", "").trim();

        return challengeRepository.findById(id).map(challenge -> {
            boolean isCorrect = challenge.getFlagAnswer().equalsIgnoreCase(submittedAnswer);

            if (isCorrect) {
                user.setTotalXp(user.getTotalXp() + challenge.getPointValue());
                user.getCompletedChallengeIds().add(challenge.getId());
                userRepository.save(user);

                if (submittedAnswer.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    DynamicFirewallFilter.blockIp(submittedAnswer);
                }

                SecurityLog successLog = new SecurityLog();
                successLog.setEventType("DEFENSIVE_TRAINING_PASSED");
                successLog.setSeverity("LOW");
                successLog.setDescription("Operative neutralized threat vector: " + challenge.getTitle() + " (IOC: " + submittedAnswer + ")");
                successLog.setTargetEmail(user.getEmail());
                successLog.setResolved(true);
                securityLogRepository.save(successLog);

                return ResponseEntity.ok(Map.<String, Object>of(
                    "status", "SUCCESS",
                    "pointsEarned", challenge.getPointValue(),
                    "totalXp", user.getTotalXp(),
                    "message", "IOC verified. Dynamic packet filter rule established."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.<String, Object>of(
                    "status", "FAILED",
                    "message", "Incorrect flag or vector analysis. Re-evaluate headers and payload."
                ));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        return userRepository.findAll().stream()
            .filter(u -> "ROLE_EMPLOYEE".equals(u.getRole()))
            .sorted((a, b) -> Integer.compare(b.getTotalXp(), a.getTotalXp()))
            .map(u -> Map.<String, Object>of(
                "fullName", u.getFullName(),
                "email", u.getEmail(),
                "totalXp", u.getTotalXp(),
                "questsCompleted", u.getCompletedChallengeIds().size()
            ))
            .toList();
    }
}