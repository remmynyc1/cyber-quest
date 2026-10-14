package com.example.demo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

public class Dtos {
    private Dtos() {}

    public record ChallengeView(Long id, String title, String scenario, String category, int pointValue, String requiredTool, String artifactName) {
        static ChallengeView of(QuestChallenge c) {
            return new ChallengeView(c.getId(), c.getTitle(), c.getScenario(), c.getCategory(), c.getPointValue(), c.getRequiredTool(), c.getArtifactName());

        }
    }
    public record UserView(Long id, String fullName, String email, String role, boolean enrolledInStarterQuest, int totalXp) {
        static UserView of(UserAccount u ){
            return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnrolledInStarterQuest(), u.getTotalXp());
        }
    }
    public record CreateUserRequest(@NotBlank @Size(max = 100) String fullName, @NotBlank @Email @Size(max = 150) String email, @NotBlank String role) {}

    public record PublicLeaderEntry(String displayName, int totalXp, int questsCompleted){}

    public record ManagerLeaderEntry(String fullName, String email, int totalXp, int questCompleted) {}

    public static String displayName(String full) {
        if (full == null || full.isBlank())return "Operative";
    String[] p = full.trim().split("\\s+");
return p.length == 1 ? p[0] : p[0] + " " + p[p.length-1].charAt(0) + ".";
    }
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnpqrstuvwxy23456789";
    private static final SecureRandom RNG = new SecureRandom();

    public static String tempPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i =0; i < 14; i++)
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        return sb.toString();
    }
public static Map<String, Object> m(Object... kv) {
    Map<String, Object> r = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
    return r; 
}
}
