package com.rituals.backend.service;

import com.rituals.backend.entity.UserHabit;
import com.rituals.backend.repository.UserHabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitReminderService {

    private final UserHabitRepository userHabitRepository;
    private final EmailService emailService;

    // Run every 15 minutes to check for habits that need reminders
    @Scheduled(fixedRate = 900000) // 15 minutes in ms
    public void sendScheduledReminders() {
        LocalTime now = LocalTime.now();
        // Check habits within a 15-minute window
        LocalTime windowStart = now.minusMinutes(7);
        LocalTime windowEnd = now.plusMinutes(8);

        List<UserHabit> allActive = userHabitRepository.findAllActiveWithUser();
        
        for (UserHabit habit : allActive) {
            if (habit.getPreferredStartTime() != null) {
                LocalTime habitTime = habit.getPreferredStartTime();
                if (!habitTime.isBefore(windowStart) && !habitTime.isAfter(windowEnd)) {
                    try {
                        String email = habit.getUser().getEmail();
                        String username = habit.getUser().getUsername();
                        String formattedTime = habit.getPreferredStartTime().toString();
                        
                        System.out.println("[REMINDER] Sending reminder for habit: " + habit.getName() + " to " + email);
                        emailService.sendHabitReminder(email, username, habit.getName(), formattedTime);
                    } catch (Exception e) {
                        System.out.println("[REMINDER] Failed to send reminder for habit " + habit.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
