package com.example.demo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "sim_attempts")
public class SimulationAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userEmail;
    private String scenarioId;
    private String kind; 
    private String currentNode;
    private int score;
    private int maxScore;
    private int riskyChoices;
    private int xpEarned;
    private String status = "IN_PROGRESS";
    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public Long getId() { return id;}
    public String getUserEmail() { return userEmail;}
    public void setUserEmail(String v ) { userEmail =v;}
    public String getScenarioId() { return scenarioId;}
    public void setScenarioId(String v) { scenarioId =v;}
    public String getKind() { return kind;}
    public void setKind(String v) { kind =v;}
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String v) { currentNode = v; }
    public int getScore() { return score; }
    public void setScore(int v) { score = v; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int v) { maxScore = v; }
    public int getRiskyChoices() { return riskyChoices; }
    public void setRiskyChoices(int v) { riskyChoices = v; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int v) { xpEarned = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { completedAt = v; }
}
