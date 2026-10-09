package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling 
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository, 
                                      PasswordEncoder passwordEncoder, 
                                      QuestChallengeRepository challengeRepository) {
        return args -> {
            // Seed Manager Account
            if (!userRepository.existsByEmail("manager@cyberquest.internal")) {
                UserAccount manager = new UserAccount();
                manager.setFullName("Inspector Chief");
                manager.setEmail("manager@cyberquest.internal");
                manager.setRole("ROLE_MANAGER");
                manager.setPasswordHash(passwordEncoder.encode("manager26"));
                manager.setEnrolledInStarterQuest(true);
                userRepository.save(manager);
                System.out.println(">>> SEEDED MANAGER ACCOUNT: manager@cyberquest.internal / manager26");
            }

            // Seed Employee Account
            if (!userRepository.existsByEmail("employee@cyberquest.internal")) {
                UserAccount employee = new UserAccount();
                employee.setFullName("Operative Trainee");
                employee.setEmail("employee@cyberquest.internal");
                employee.setRole("ROLE_EMPLOYEE");
                employee.setPasswordHash(passwordEncoder.encode("employee26"));
                employee.setEnrolledInStarterQuest(true);
                userRepository.save(employee);
                System.out.println(">>> SEEDED EMPLOYEE ACCOUNT: employee@cyberquest.internal / employee26");
            }

            // Seed Defensive Quest Challenges
            if (challengeRepository.count() == 0) {
                challengeRepository.save(new QuestChallenge(
                    "Phishing Header Analysis",
                    "Inspect the incoming message headers: Received: from mail-attacker.org (IP: 198.51.100.23). Identify the spoofed source IP.",
                    "PHISHING",
                    "198.51.100.23",
                    100
                ));
                challengeRepository.save(new QuestChallenge(
                    "Default Password Exploitation",
                    "A remote management console at 192.168.12.1 is exposing admin endpoints. Identify the default administrative credential pair.",
                    "CREDENTIALS",
                    "admin:admin",
                    150
                ));
                System.out.println(">>> SEEDED QUEST CHALLENGES");
            }
        };
    }
}
