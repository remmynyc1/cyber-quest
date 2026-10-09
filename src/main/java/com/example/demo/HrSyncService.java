package com.example.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HrSyncService {

    private final JdbcTemplate hrJdbcTemplate;
    private final UserRepository userRepository;
    private final QuestScheduleService questScheduleService;

    public HrSyncService(@Qualifier("hrJdbcTemplate") JdbcTemplate hrJdbcTemplate, 
                         UserRepository userRepository, 
                         QuestScheduleService questScheduleService) {
        this.hrJdbcTemplate = hrJdbcTemplate;
        this.userRepository = userRepository;
        this.questScheduleService = questScheduleService;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncNewEmployees() {
        String selectSql = "SELECT id, full_name, email, department, role, sync_status FROM employees WHERE sync_status = 'PENDING'";
        
        List<HrEmployee> pendingEmployees = hrJdbcTemplate.query(selectSql, (rs, rowNum) -> {
            HrEmployee emp = new HrEmployee();
            emp.setId(rs.getLong("id"));
            emp.setFullName(rs.getString("full_name"));
            emp.setEmail(rs.getString("email"));
            emp.setDepartment(rs.getString("department"));
            emp.setRole(rs.getString("role"));
            emp.setSyncStatus(rs.getString("sync_status"));
            return emp;
        });

        for (HrEmployee emp : pendingEmployees) {
            if (!userRepository.existsByEmail(emp.getEmail())) {
                UserAccount account = new UserAccount();
                account.setFullName(emp.getFullName());
                account.setEmail(emp.getEmail());
                account.setRole("ROLE_" + emp.getRole().toUpperCase());
                account.setPasswordHash("$2a$10$DefaultHashedPasswordPlaceHolderStringForMockPurposes");
                account.setEnrolledInStarterQuest(true);

                userRepository.save(account);
                questScheduleService.scheduleStarterQuest(account, LocalDateTime.now().withHour(10));
            }

            String updateSql = "UPDATE employees SET sync_status = 'PROCESSED' WHERE id = ?";
            hrJdbcTemplate.update(updateSql, emp.getId());
        }
    }
}
