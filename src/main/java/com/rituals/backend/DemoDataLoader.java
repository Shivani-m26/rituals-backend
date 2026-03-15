package com.rituals.backend;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.UserHabit;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.service.HabitTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

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
        System.out.println("====== RUNNING DEMO DATA LOADER ======");
        // Get an existing user or create one
        AppUser user = userRepository.findByEmail("test2@example.com").orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setEmail("test2@example.com");
            newUser.setPassword(passwordEncoder.encode("password")); // hashed
            newUser.setUsername("demouser");
            newUser.setTotalPoints(0);
            return userRepository.save(newUser);
        });

        // Delete any existing habits for demo user to keep it clean
        System.out.println("Setting up demo habits for: " + user.getEmail());

        // Create a habit that started 5 days ago
        LocalDate startDate = LocalDate.now().minusDays(5);
        UserHabit habit = habitTrackingService.startCustomHabit(
                user,
                "Morning Meditation",
                "Deep breathing for 10 minutes",
                "Mindfulness",
                "Intermediate",
                "30",
                LocalTime.of(7, 0),
                30, 
                startDate
        );

        // Manually log for the past 5 days
        for (int i = 0; i < 5; i++) {
            LocalDate logDate = startDate.plusDays(i);
            boolean completed = (i != 2); // Missed day 3
            String remark = completed ? "Felt peaceful." : "Forgot to do it.";
            try {
                habitTrackingService.logProgress(habit.getId(), logDate, remark, completed);
                System.out.println("Logged " + logDate + ": " + completed);
            } catch (Exception e) {
                System.out.println("Failed to log " + logDate + ": " + e.getMessage());
            }
        }
        System.out.println("====== DEMO DATA LOADER FINISHED ======");
    }
}
