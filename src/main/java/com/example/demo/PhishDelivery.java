package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "phish_deliveries")
public class PhishDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long campaignId;
    private String userEmail;
    private LocalDateTime deliveredAt =  LocalDateTime.now();
    private LocalDateTime clickedAt;
    private LocalDateTime reportedAt;
    
    public Long getId() { return id;}
    public Long getCampaignId() { return campaignId;}
    public void setCampaignId(Long v) { campaignId = v;}
    public String getUserEmail() { return userEmail;}
    public void setUserEmail(String v) { userEmail = v;}
    public LocalDateTime getDeliveredAt() { return deliveredAt;}
    public LocalDateTime getClickedAt() { return clickedAt;}
    public void setClickedAt(LocalDateTime v) { clickedAt = v;}
    public LocalDateTime getReportedAt() { return reportedAt;}
    public void setReportedAt(LocalDateTime v) { reportedAt = v;}
}
