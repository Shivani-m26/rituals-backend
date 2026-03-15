package com.rituals.backend.scheduler;

import com.rituals.backend.entity.UserHabit;
import com.rituals.backend.repository.UserHabitRepository;
import com.rituals.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final UserHabitRepository userHabitRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 * * * * *") // Run every minute
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String currentHourMinute = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<UserHabit> activeHabits = userHabitRepository.findAllActiveWithUser();
        
        for (UserHabit habit : activeHabits) {
            // Check if today is on or after the start date
            if (habit.getStartDate() != null && !today.isBefore(habit.getStartDate())) {
                // If the habit has an end date, check if today is on or before the end date
                if (habit.getEndDate() == null || !today.isAfter(habit.getEndDate())) {
                    if (habit.getPreferredStartTime() != null) {
                        String habitTime = habit.getPreferredStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                        
                        if (currentHourMinute.equals(habitTime)) {
                            emailService.sendHabitReminder(
                                habit.getUser().getEmail(),
                                habit.getUser().getUsername(),
                                habit.getName(),
                                habitTime
                            );
                        }
                    }
                }
            }
        }
    }
}
