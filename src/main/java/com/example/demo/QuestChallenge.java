package com.example.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "quest_challenges")
public class QuestChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String scenario;

    private String category;
    private String flagAnswer;
    private int pointValue;
    
    // Real tool integration fields
    private String requiredTool;   // e.g., "Wireshark", "Linux Terminal", "CyberChef", "Burp Suite"
    private String artifactName;   // e.g., "suspicious_traffic.pcap", "auth_audit.log"

    public QuestChallenge() {}

    public QuestChallenge(String title, String scenario, String category, 
                          String flagAnswer, int pointValue, 
                          String requiredTool, String artifactName) {
        this.title = title;
        this.scenario = scenario;
        this.category = category;
        this.flagAnswer = flagAnswer;
        this.pointValue = pointValue;
        this.requiredTool = requiredTool;
        this.artifactName = artifactName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFlagAnswer() { return flagAnswer; }
    public void setFlagAnswer(String flagAnswer) { this.flagAnswer = flagAnswer; }
    public int getPointValue() { return pointValue; }
    public void setPointValue(int pointValue) { this.pointValue = pointValue; }
    public String getRequiredTool() { return requiredTool; }
    public void setRequiredTool(String requiredTool) { this.requiredTool = requiredTool; }
    public String getArtifactName() { return artifactName; }
    public void setArtifactName(String artifactName) { this.artifactName = artifactName; }
}