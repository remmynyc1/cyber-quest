package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface PhishCampaignRepository extends JpaRepository<PhishCampaign, Long> {
    List<PhishCampaign> findAllByOrderByLaunchedAtDesc();
    
}
