package com.rituals.backend.controller;

import com.rituals.backend.entity.*;
import com.rituals.backend.repository.*;
import com.rituals.backend.service.HabitTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AppUserRepository userRepository;
    private final UserHabitRepository habitRepository;
    private final TrackerLogRepository logRepository;
    private final BadgeRepository badgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final HabitTrackingService trackingService;

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemo() {
        // 1. Create User A: RitualSage (Male)
        AppUser sage = userRepository.findByEmail("sage@example.com").orElseGet(() -> {
            AppUser u = AppUser.builder()
                    .email("sage@example.com")
                    .username("SageMaster")
                    .password(passwordEncoder.encode("password"))
                    .gender("Male")
                    .totalPoints(105) // 21 * 5
                    .build();
            return userRepository.save(u);
        });

        // Seed Habit for User A: Deep Work (Gold Mastered)
        UserHabit habitA = UserHabit.builder()
                .user(sage)
                .name("Deep Work Protocol")
                .domain("Productivity")
                .difficulty("Hard")
                .planType("21")
                .startDate(LocalDate.now().minusDays(21))
                .endDate(LocalDate.now())
                .isActive(false)
                .totalCompletions(21)
                .totalPointsAttained(105)
                .build();
        habitA = habitRepository.save(habitA);

        // Add 21 Success Logs
        for (int i = 0; i < 21; i++) {
            logRepository.save(TrackerLog.builder()
                    .userHabit(habitA)
                    .date(LocalDate.now().minusDays(21-i))
                    .isCompleted(true)
                    .pointsChanged(5)
                    .remark("Consistency is key.")
                    .build());
        }

        // Award Gold Badge to Sage
        badgeRepository.save(Badge.builder()
                .user(sage)
                .name("GOLD Ritual Master")
                .tier("GOLD")
                .earnedAt(LocalDate.now())
                .habitReferenceName("Deep Work Protocol")
                .build());

        // 2. Create User B: SkipperD (Female)
        AppUser skipper = userRepository.findByEmail("skipper@example.com").orElseGet(() -> {
            AppUser u = AppUser.builder()
                    .email("skipper@example.com")
                    .username("SkipperDiscipline")
                    .password(passwordEncoder.encode("password"))
                    .gender("Female")
                    .totalPoints(75) // 19*5 - 2*10 = 95 - 20 = 75
                    .build();
            return userRepository.save(u);
        });

        // Seed Habit for User B: Creative Flow (Skips detected)
        UserHabit habitB = UserHabit.builder()
                .user(skipper)
                .name("Creative Flow Journey")
                .domain("Art & Creativity")
                .difficulty("Medium")
                .planType("21")
                .startDate(LocalDate.now().minusDays(21))
                .endDate(LocalDate.now())
                .isActive(true)
                .totalCompletions(19)
                .totalPointsAttained(75)
                .totalPointsLost(20) // 2 days skipped
                .build();
        habitB = habitRepository.save(habitB);

        // Add 19 Success Logs and 2 Skips
        for (int i = 0; i < 21; i++) {
            boolean completed = i != 5 && i != 12; // Skip 2 days
            logRepository.save(TrackerLog.builder()
                    .userHabit(habitB)
                    .date(LocalDate.now().minusDays(21-i))
                    .isCompleted(completed)
                    .pointsChanged(completed ? 5 : -10)
                    .remark(completed ? "Inspired!" : "Life got in the way...")
                    .build());
        }

        return ResponseEntity.ok("Demo seeded! Login as sage@example.com or skipper@example.com (password: password)");
    }
}
