package com.example.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class HrSyncService {

    /** HR data can never create a manager. Managers are created by an existing manager only. */
    private static final Set<String> HR_ALLOWED_ROLES = Set.of("EMPLOYEE", "ANALYST");

    private final JdbcTemplate hrJdbcTemplate;
    private final UserRepository userRepository;
    private final QuestScheduleService questScheduleService;
    private final PasswordEncoder passwordEncoder;

    public HrSyncService(@Qualifier("hrJdbcTemplate") JdbcTemplate hrJdbcTemplate,
                         UserRepository userRepository,
                         QuestScheduleService questScheduleService,
                         PasswordEncoder passwordEncoder) {
        this.hrJdbcTemplate = hrJdbcTemplate;
        this.userRepository = userRepository;
        this.questScheduleService = questScheduleService;
        this.passwordEncoder = passwordEncoder;
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
            String email = emp.getEmail() == null ? "" : emp.getEmail().trim().toLowerCase();
            if (!email.isEmpty() && !userRepository.existsByEmail(email)) {
                String role = emp.getRole() == null ? "EMPLOYEE" : emp.getRole().toUpperCase().replace("ROLE_", "");
                if (!HR_ALLOWED_ROLES.contains(role)) role = "EMPLOYEE";

                UserAccount account = new UserAccount();
                account.setFullName(emp.getFullName());
                account.setEmail(email);
                account.setRole("ROLE_" + role);
                // Unknown random password: a manager issues a real one with "Reset password".
                account.setPasswordHash(passwordEncoder.encode(Dtos.tempPassword()));
                account.setEnrolledInStarterQuest(true);
                account.setMustChangePassword(true);

                userRepository.save(account);
                questScheduleService.scheduleStarterQuest(account, LocalDateTime.now().withHour(10));
            }

            hrJdbcTemplate.update("UPDATE employees SET sync_status = 'PROCESSED' WHERE id = ?", emp.getId());
        }
    }
}
