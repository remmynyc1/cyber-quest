package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhishDeliveryRepository extends JpaRepository<PhishDelivery, Long>{
    List<PhishDelivery> findByUserEmailOrderByDeliveredAtDesc(String email);
    List<PhishDelivery> findByCampaignId(Long campaignId);
    
}
