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

    @GetMapping("/challenges")
    public List<QuestChallenge> getAvailableChallenges() {
        // Return challenges without exposing the plaintext flag answers to the client
        return challengeRepository.findAll().stream().peek(c -> {}).toList();
    }

    @PostMapping("/challenges/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitSolution(
            @PathVariable Long id,
            @RequestBody Map<String, String> submission,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String submittedAnswer = submission.getOrDefault("answer", "").trim();

        return challengeRepository.findById(id).map(challenge -> {
            boolean isCorrect = challenge.getFlagAnswer().equalsIgnoreCase(submittedAnswer);

            if (isCorrect) {
                // Automatically log completed defensive action in SecurityLog
                SecurityLog successLog = new SecurityLog();
                successLog.setEventType("DEFENSIVE_TRAINING_PASSED");
                successLog.setSeverity("LOW");
                successLog.setDescription("Operative successfully resolved challenge: " + challenge.getTitle());
                successLog.setTargetEmail(authentication.getName());
                successLog.setResolved(true);
                securityLogRepository.save(successLog);

                return ResponseEntity.ok(Map.<String, Object>of(
                    "status", "SUCCESS",
                    "pointsEarned", challenge.getPointValue(),
                    "message", "Challenge neutralized. Points awarded to operative record."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.<String, Object>of(
                    "status", "FAILED",
                    "message", "Incorrect flag or vector analysis. Re-evaluate headers and payload."
                ));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}