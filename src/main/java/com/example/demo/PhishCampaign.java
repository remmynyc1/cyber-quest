package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "phish_campaigns")
public class PhishCampaign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String templateId;
    private String launchedBy;
    private LocalDateTime launchedAt = LocalDateTime.now();

    public Long getId() { return id;}
    public String getName() { return name;}
    public void setName(String v) { name = v; }
    public String getTemplateId() { return templateId;}
    public void setTemplateId(String v) { templateId = v;}
    public String getLaunchedBy() { return launchedBy;}
    public void setLaunchedBy(String v) { launchedBy = v;}
    public LocalDateTime getLaunchedAt() { return launchedAt;}

}
