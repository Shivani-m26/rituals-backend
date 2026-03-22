package com.rituals.backend;

import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.service.HabitTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final HabitTrackingService habitTrackingService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Disabled in production.
        // Creates duplicate habits for demo user on every restart.
        // The 600 master habits and leaderboard mock users are handled by DataInitializer.
        System.out.println("====== DEMO DATA LOADER SKIPPED (disabled for production) ======");
    }
}
