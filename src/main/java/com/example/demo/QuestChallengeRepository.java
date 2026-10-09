package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestChallengeRepository extends JpaRepository<QuestChallenge, Long> {
    List<QuestChallenge> findByCategory(String category);
}