package com.rituals.backend.scheduler;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.UserHabit;
import com.rituals.backend.repository.UserHabitRepository;
import com.rituals.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HabitReminderScheduler {

    private final UserHabitRepository userHabitRepository;
    private final EmailService emailService;

    /**
     * Daily morning reminder job running at 09:00 AM server time.
     * Finds all users with active rituals and sends them an inspiring daily reminder to stay on track.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyMorningRitualReminders() {
        log.info("[SCHEDULER] Running daily morning ritual reminders...");
        sendRemindersForActiveHabits();
    }

    /**
     * Daily evening reminder job running at 08:00 PM (20:00) server time.
     * Gentle nudge to complete any remaining rituals before the day ends.
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendDailyEveningRitualReminders() {
        log.info("[SCHEDULER] Running daily evening ritual reminders...");
        sendRemindersForActiveHabits();
    }

    private void sendRemindersForActiveHabits() {
        try {
            List<UserHabit> activeHabits = userHabitRepository.findAllActiveWithUser();
            if (activeHabits.isEmpty()) {
                log.info("[SCHEDULER] No active habits found for reminder.");
                return;
            }

            // Group active habits by user
            Map<AppUser, List<UserHabit>> habitsByUser = activeHabits.stream()
                    .filter(h -> h.getUser() != null && h.getUser().getEmail() != null)
                    .collect(Collectors.groupingBy(UserHabit::getUser));

            for (Map.Entry<AppUser, List<UserHabit>> entry : habitsByUser.entrySet()) {
                AppUser user = entry.getKey();
                List<UserHabit> userHabits = entry.getValue();

                for (UserHabit habit : userHabits) {
                    // Check if habit is currently within its active date range
                    LocalDate now = LocalDate.now();
                    if (habit.getStartDate() != null && now.isBefore(habit.getStartDate())) {
                        continue;
                    }
                    if (habit.getEndDate() != null && now.isAfter(habit.getEndDate())) {
                        continue;
                    }

                    String timeStr = habit.getPreferredStartTime() != null 
                            ? habit.getPreferredStartTime().toString() 
                            : "Today";

                    emailService.sendHabitReminder(
                            user.getEmail(),
                            user.getUsername(),
                            habit.getName(),
                            timeStr
                    );
                }
            }
            log.info("[SCHEDULER] Dispatched reminders to {} users.", habitsByUser.size());
        } catch (Exception e) {
            log.error("[SCHEDULER] Error dispatching habit reminders: {}", e.getMessage(), e);
        }
    }
}

