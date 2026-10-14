package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XpService {
    private final UserRepository users;

    public XpService(UserRepository users) { this.users = users; }

    @Transactional
    public int award(String email, int xp) {
        if (xp <= 0) return 0;
        users.findByEmail(email).ifPresent(u -> {
            u.setTotalXp(u.getTotalXp() + xp);
            users.save(u);
        });
        return xp;
    }
}
