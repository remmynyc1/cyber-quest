package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String role;
    private String passwordHash;
    private boolean enrolledInStarterQuest;

    @Column(name = "total_xp")
    private Integer totalXp = 0;

    private Boolean mustChangePassword = false;
    private Integer failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_completed_challenges", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "challenge_id")
    private Set<Long> completedChallengeIds = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEnrolledInStarterQuest() { return enrolledInStarterQuest; }
    public void setEnrolledInStarterQuest(boolean enrolledInStarterQuest) { this.enrolledInStarterQuest = enrolledInStarterQuest; }

    public int getTotalXp() { return totalXp != null ? totalXp : 0; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp != null ? totalXp : 0; }

    public Set<Long> getCompletedChallengeIds() { return completedChallengeIds; }
    public void setCompletedChallengeIds(Set<Long> completedChallengeIds) { this.completedChallengeIds = completedChallengeIds; }

public boolean isMustChangePassword() { return Boolean.TRUE.equals(mustChangePassword); }
public void setMustChangePassword(boolean v) { this.mustChangePassword = v; }
public int getFailedLoginAttempts() { return failedLoginAttempts != null ? failedLoginAttempts : 0; }
public void setFailedLoginAttempts(int v) { this.failedLoginAttempts = v; }
public LocalDateTime getLockedUntil() { return lockedUntil; }
public void setLockedUntil(LocalDateTime v) { this.lockedUntil = v; }

}