package com.example.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "quest_challenges")
public class QuestChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(length = 1000)
    private String scenario;
    private String category;      // PHISHING, NETWORK, CREDENTIALS
    private String flagAnswer;    // Expected answer/flag
    private int pointValue;

    public QuestChallenge() {}

    public QuestChallenge(String title, String scenario, String category, String flagAnswer, int pointValue) {
        this.title = title;
        this.scenario = scenario;
        this.category = category;
        this.flagAnswer = flagAnswer;
        this.pointValue = pointValue;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getScenario() { return scenario; }
    public String getCategory() { return category; }
    public String getFlagAnswer() { return flagAnswer; }
    public int getPointValue() { return pointValue; }
}