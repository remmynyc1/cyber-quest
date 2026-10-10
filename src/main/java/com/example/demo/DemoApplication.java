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
                manager.setTotalXp(0);
                userRepository.save(manager);
            }

            // Seed Employee Account
            if (!userRepository.existsByEmail("employee@cyberquest.internal")) {
                UserAccount employee = new UserAccount();
                employee.setFullName("Operative Trainee");
                employee.setEmail("employee@cyberquest.internal");
                employee.setRole("ROLE_EMPLOYEE");
                employee.setPasswordHash(passwordEncoder.encode("employee26"));
                employee.setEnrolledInStarterQuest(true);
                employee.setTotalXp(0);
                userRepository.save(employee);
            }

            // Seed Complete 5-Module Defensive Quest Track
            if (challengeRepository.count() == 0) {
                // Tier 1: Foundational SOC Identification
                challengeRepository.save(new QuestChallenge(
                    "Perimeter Port Reconnaissance Triage",
                    "A public-facing gateway was probed during off-hours. Analysis indicates attempts against default administrative services. Submit the default unencrypted remote access port targeted.",
                    "NETWORK_SECURITY",
                    "23",
                    100,
                    "Nmap / Port Scan Analysis",
                    "telnet_probe_alert.txt"
                ));

                challengeRepository.save(new QuestChallenge(
                    "Credential Stuffing Vector Identification",
                    "Internal IAM telemetry logged rapid failed authentication attempts across consecutive executive accounts originating from a single internal subnet IP. Submit the attacking host address (192.168.12.130).",
                    "IDENTITY_ACCESS",
                    "192.168.12.130",
                    100,
                    "SIEM Log Aggregator",
                    "auth_fail_stream.txt"
                ));

                // Tier 2: Real Forensic Tool Artifact Investigations
                challengeRepository.save(new QuestChallenge(
                    "SSH Brute Force Forensic Extraction",
                    "A public bastion server logged multiple unauthorized authentication events. Download the audit log and extract the source IP conducting the dictionary attack.",
                    "HOST_FORENSICS",
                    "203.0.113.88",
                    150,
                    "Linux Terminal (grep / awk)",
                    "auth_audit.log"
                ));

                challengeRepository.save(new QuestChallenge(
                    "Malicious Email Gateway Spoof Triage",
                    "A phishing email bypassed the perimeter filter. Download the raw .eml file, inspect the received headers, and extract the real relay IP that failed the SPF authorization check.",
                    "PHISHING_ANALYSIS",
                    "198.51.100.99",
                    150,
                    "Email Client / Text Editor",
                    "phishing_investigation.eml"
                ));

                challengeRepository.save(new QuestChallenge(
                    "Encrypted DNS Tunneling Traffic Capture",
                    "A network tap captured an anomalous outbound burst. Download the .pcap capture, filter the DNS traffic in Wireshark, and extract the spoofed sub-domain key prefixed before .cyberquest.",
                    "NETWORK_TRAFFIC",
                    "c2-key-992",
                    250,
                    "Wireshark / TShark",
                    "dns_exfil_traffic.pcap"
                ));
                System.out.println(">>> SEEDED 5-MODULE DEFENSIVE QUEST TRACK (750 MAX XP)");
            }
        };
    }
}