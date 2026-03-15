package com.rituals.backend.controller;

import com.rituals.backend.entity.*;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.repository.MasterHabitRepository;
import com.rituals.backend.repository.UserHabitRepository;
import com.rituals.backend.service.HabitTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitTrackingController {

    private final UserHabitRepository habitRepository;
    private final MasterHabitRepository masterHabitRepository;
    private final AppUserRepository userRepository;
    private final HabitTrackingService trackingService;

    @GetMapping("/active")
    public ResponseEntity<List<UserHabit>> getActiveHabits(Authentication authentication) {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(habitRepository.findByUserIdAndIsActiveTrue(user.getId()));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startHabit(@RequestBody Map<String, Object> request, Authentication authentication) {
        System.out.println("[DEBUG] /api/habits/start received: " + request);
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Long masterHabitId = Long.valueOf(request.get("masterHabitId").toString());
        String planType = request.get("planType").toString();
        LocalTime startTime = LocalTime.parse(request.get("startTime").toString());

        MasterHabitCatalog master = masterHabitRepository.findById(masterHabitId).orElseThrow();
        LocalDate startDate = request.containsKey("startDate") ? LocalDate.parse(request.get("startDate").toString()) : LocalDate.now();
        UserHabit habit = trackingService.startHabit(user, master, planType, startTime, startDate);
        
        System.out.println("[DEBUG] Habit started successfully: " + habit.getId());
        return ResponseEntity.ok(habit);
    }

    @PostMapping("/start-custom")
    public ResponseEntity<?> startCustomHabit(@RequestBody Map<String, Object> request, Authentication authentication) {
        System.out.println("[DEBUG] /api/habits/start-custom received: " + request);
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        String name = request.get("name").toString();
        String description = request.get("description").toString();
        String domain = request.get("domain").toString();
        String difficulty = request.get("difficulty").toString();
        String planType = request.get("planType").toString();
        LocalTime startTime = LocalTime.parse(request.get("startTime").toString());
        int duration = Integer.parseInt(request.get("duration").toString());
        LocalDate startDate = request.containsKey("startDate") ? LocalDate.parse(request.get("startDate").toString()) : LocalDate.now();

        UserHabit habit = trackingService.startCustomHabit(user, name, description, domain, difficulty, planType, startTime, duration, startDate);
        System.out.println("[DEBUG] Custom habit started successfully: " + habit.getId());
        return ResponseEntity.ok(habit);
    }

    @PostMapping("/{id}/log")
    public ResponseEntity<?> logProgress(@PathVariable("id") Long id, @RequestBody Map<String, Object> request) {
        String remark = (String) request.get("remark");
        boolean completed = (boolean) request.get("completed");
        try {
            LocalDate date = request.containsKey("date") ? LocalDate.parse(request.get("date").toString()) : LocalDate.now();
            TrackerLog log = trackingService.logProgress(id, date, remark, completed);
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TrackerLog>> getHabitLogs(@PathVariable("id") Long id) {
        return ResponseEntity.ok(trackingService.getLogs(id));
    }

    @PostMapping("/{id}/claim-badge")
    public ResponseEntity<?> claimBadge(@PathVariable("id") Long id) {
        try {
            Badge badge = trackingService.claimBadge(id);
            return ResponseEntity.ok(badge);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHabit(@PathVariable("id") Long id) {
        try {
            trackingService.deleteHabit(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
