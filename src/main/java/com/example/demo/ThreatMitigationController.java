package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/threats")
public class ThreatMitigationController {

    private final SecurityLogRepository logRepository;
    private static final String REQUIRED_HEX = "#E63946";
    private static boolean threatActive = true;

    public ThreatMitigationController(SecurityLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAnomalyStatus() {
        return ResponseEntity.ok(Map.of(
            "threatId", "THREAT-084-INFILTRATION",
            "requiredHex", REQUIRED_HEX,
            "status", threatActive ? "ACTIVE" : "RESOLVED",
            "active", threatActive
        ));
    }

    @PostMapping("/verify-key")
    public ResponseEntity<Map<String, Object>> verifyKey(@RequestBody Map<String, String> payload) {
        String submittedHex = payload.getOrDefault("hexCode", "").trim().toUpperCase();

        if (!threatActive) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ALREADY_RESOLVED",
                "message", "THREAT-084-INFILTRATION has already been neutralized."
            ));
        }

        if (REQUIRED_HEX.equalsIgnoreCase(submittedHex)) {
            threatActive = false;

            SecurityLog log = new SecurityLog();
            log.setEventType("OUT_OF_BAND_KEY_VERIFIED");
            log.setSeverity("LOW");
            log.setDescription("THREAT-084 neutralized via physical optical capture (Color Key: " + submittedHex + ")");
            log.setTargetEmail("soc-supervisor@cyberquest.internal");
            log.setResolved(true);
            logRepository.save(log);

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Optical color spectrum verified. Firewall mitigation rule committed.",
                "threatId", "THREAT-084-INFILTRATION"
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
            "status", "FAILED",
            "message", "Color spectrum mismatch (" + submittedHex + "). Threat remains active."
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetThreat() {
        threatActive = true;
        return ResponseEntity.ok(Map.of("message", "THREAT-084 reset to active."));
    }
}