package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SimulationAttemptRepository extends JpaRepository<SimulationAttempt, Long> {
    List<SimulationAttempt> findByUserEmail(String email);
    List<SimulationAttempt> findByUserEmailAndScenarioId(String email, String scenarioId);
    List<SimulationAttempt> findByUserEmailAndScenarioIdAndStatus(String email, String scenarioId, String status);
    List<SimulationAttempt> findByStatus(String status);
}
