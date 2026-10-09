package com.example.demo;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Service
public class QuestScheduleService {

    private static final Logger LOGGER = Logger.getLogger(QuestScheduleService.class.getName());

    // Simulates scheduling a starter quest session while checking for calendar conflicts
    public boolean scheduleStarterQuest(UserAccount user, LocalDateTime requestedTime) {
        LOGGER.info("Checking schedule availability for agent: " + user.getFullName());
        
        // Basic conflict detection mock logic (e.g., avoid weekends or past hours)
        if (requestedTime.getHour() < 9 || requestedTime.getHour() > 17) {
            LOGGER.warning("Scheduling failed: Requested time is outside operating hours.");
            return false;
        }

        LOGGER.info("SUCCESS: Starter Quest successfully scheduled for " + user.getEmail() + " at " + requestedTime);
        return true;
    }
}