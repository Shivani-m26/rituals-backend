package com.rituals.backend.service;

import com.rituals.backend.entity.*;
import com.rituals.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HabitTrackingService {

    private final UserHabitRepository userHabitRepository;
    private final TrackerLogRepository trackerLogRepository;
    private final AppUserRepository userRepository;
    private final BadgeRepository badgeRepository;

    @Transactional
    public UserHabit startHabit(AppUser user, MasterHabitCatalog masterHabit, String planType, java.time.LocalTime startTime, LocalDate startDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        Integer days = planType.equalsIgnoreCase("Infinite") ? null : Integer.parseInt(planType);
        LocalDate end = days == null ? null : start.plusDays(days);

        UserHabit habit = UserHabit.builder()
                .user(user)
                .masterHabit(masterHabit)
                .name(masterHabit.getName())
                .description(masterHabit.getDescription())
                .domain(masterHabit.getDomain())
                .difficulty(masterHabit.getDifficulty())
                .planType(planType)
                .startDate(start)
                .endDate(end)
                .preferredStartTime(startTime)
                .isActive(true)
                .build();

        return userHabitRepository.save(habit);
    }

    @Transactional
    public UserHabit startCustomHabit(AppUser user, String name, String description, String domain, String difficulty, String planType, java.time.LocalTime startTime, int duration, LocalDate startDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = planType.equalsIgnoreCase("Infinite") ? null : start.plusDays(duration);

        UserHabit habit = UserHabit.builder()
                .user(user)
                .name(name)
                .description(description)
                .domain(domain)
                .difficulty(difficulty)
                .planType(planType)
                .startDate(start)
                .endDate(end)
                .preferredStartTime(startTime)
                .isActive(true)
                .build();

        return userHabitRepository.save(habit);
    }

    @Transactional
    public TrackerLog logProgress(Long userHabitId, LocalDate date, String remark, boolean completed) {
        UserHabit habit = userHabitRepository.findById(userHabitId).orElseThrow();
        
        // Production Rule: Strictly lock past and future logs
        // TEMPORARILY DISABLED FOR DEMO
        /*
        if (date.isBefore(LocalDate.now())) {
            throw new RuntimeException("This day is frozen. You cannot log progress for past days.");
        }
        */
        if (date.isAfter(LocalDate.now())) {
            throw new RuntimeException("You cannot log progress for the future. Patience is a virtue.");
        }

        Optional<TrackerLog> existing = trackerLogRepository.findByUserHabitIdAndDate(userHabitId, date);
        boolean wasCompleted = existing.map(TrackerLog::getIsCompleted).orElse(false);
        boolean isNewLog = existing.isEmpty();

        TrackerLog log = existing.orElse(TrackerLog.builder()
                .userHabit(habit)
                .date(date)
                .build());

        log.setRemark(remark);
        log.setIsCompleted(completed);
        
        int pointsChanged = 0;
        
        // Points Calculation:
        // +5 for completing (only when status changes to completed)
        // -10 for marking as not completed (only when status changes to not completed from completed)
        // For new logs: +5 if completed, 0 if marking as not done (no penalty on first log-not-done)
        if (completed && !wasCompleted) {
            // Completing the habit
            habit.setTotalCompletions(habit.getTotalCompletions() + 1);
            habit.setStreakCount(habit.getStreakCount() + 1);
            pointsChanged = 5;
            habit.setTotalPointsAttained(habit.getTotalPointsAttained() + 5);
        } else if (!completed && wasCompleted) {
            // Unchecking a previously completed habit
            habit.setTotalCompletions(Math.max(0, habit.getTotalCompletions() - 1));
            habit.setStreakCount(0);
            pointsChanged = -5; // Reverse the +5 that was given
            habit.setTotalPointsAttained(Math.max(0, habit.getTotalPointsAttained() - 5));
            habit.setTotalPointsLost(habit.getTotalPointsLost() + 5);
        } else if (!completed && !wasCompleted && !isNewLog) {
            // Re-logging an existing not-completed entry: no additional penalty
            pointsChanged = 0;
        }
        // If completed && wasCompleted: no change (already counted)
        // If new log && not completed: no penalty (user just opened the log)

        log.setPointsChanged(pointsChanged);
        userHabitRepository.save(habit);
        
        // Update Global User Total Points
        if (pointsChanged != 0) {
            AppUser user = habit.getUser();
            user.setTotalPoints(user.getTotalPoints() + pointsChanged);
            userRepository.save(user);
        }

        return trackerLogRepository.save(log);
    }

    @Transactional
    public Badge claimBadge(Long userHabitId) {
        UserHabit habit = userHabitRepository.findById(userHabitId).orElseThrow();
        AppUser user = habit.getUser();

        if (!habit.getIsActive()) {
            throw new RuntimeException("This ritual is already archived.");
        }

        // Calculate Penalty Fee (2x points lost)
        int penaltyFee = habit.getTotalPointsLost() * 2;
        if (user.getTotalPoints() < penaltyFee) {
            throw new RuntimeException("Insufficient Wisdom Points to claim this badge. Required: " + penaltyFee);
        }

        // Deduct Fee
        user.setTotalPoints(user.getTotalPoints() - penaltyFee);
        userRepository.save(user);

        // Determine Tier based on plan type
        String tier = "SILVER";
        if (habit.getPlanType().equals("21")) tier = "GOLD";
        else if (habit.getPlanType().equals("48") || habit.getPlanType().equalsIgnoreCase("Infinite")) tier = "PLATINUM";

        Badge badge = Badge.builder()
                .user(user)
                .name(tier + " Ritual Master")
                .tier(tier)
                .earnedAt(LocalDate.now())
                .habitReferenceName(habit.getName())
                .build();

        badge = badgeRepository.save(badge);

        // Archive Habit
        habit.setIsActive(false);
        userHabitRepository.save(habit);

        // Check for Prime Evolution (5 of same tier)
        checkAndAwardPrimeBadge(user, tier);

        return badge;
    }

    private void checkAndAwardPrimeBadge(AppUser user, String tier) {
        long count = badgeRepository.findByUserId(user.getId()).stream()
                .filter(b -> b.getTier().equals(tier) && !b.getIsPrime())
                .count();

        if (count >= 5) {
            Badge prime = Badge.builder()
                    .user(user)
                    .name(tier + " PRIME Ritualist")
                    .tier(tier)
                    .isPrime(true)
                    .earnedAt(LocalDate.now())
                    .habitReferenceName("Legendary consistency in " + tier)
                    .build();
            badgeRepository.save(prime);
        }
    }

    @Transactional
    public void deleteHabit(Long habitId) {
        trackerLogRepository.deleteByUserHabitId(habitId);
        userHabitRepository.deleteById(habitId);
    }

    public List<TrackerLog> getLogs(Long userHabitId) {
        return trackerLogRepository.findByUserHabitId(userHabitId);
    }
}
