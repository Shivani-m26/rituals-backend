package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "user_habits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHabit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_habit_id")
    private MasterHabitCatalog masterHabit;

    @Column(nullable = false)
    private String name;

    private String description;
    private String domain;
    private String difficulty;

    @Column(name = "plan_type", nullable = false)
    private String planType; // 14, 21, 48, Infinite

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date") // Can be null for infinite
    private LocalDate endDate;

    @Column(name = "preferred_start_time")
    private java.time.LocalTime preferredStartTime;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "streak_count")
    @Builder.Default
    private Integer streakCount = 0;

    @Column(name = "total_completions")
    @Builder.Default
    private Integer totalCompletions = 0;

    @Column(name = "total_points_attained")
    @Builder.Default
    private Integer totalPointsAttained = 0;

    @Column(name = "total_points_lost")
    @Builder.Default
    private Integer totalPointsLost = 0; // Cumulative penalties for skipped days

    // Inverse relationship for TrackerLogs
    @OneToMany(mappedBy = "userHabit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("date ASC")
    private List<TrackerLog> trackerLogs;
}
