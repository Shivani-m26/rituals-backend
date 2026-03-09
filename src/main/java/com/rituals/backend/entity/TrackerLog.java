package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tracker_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_habit_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private UserHabit userHabit;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "points_changed")
    private Integer pointsChanged; // +5 or -10

    @Column(length = 250)
    private String remark; // User's feeling about doing it (max 250 characters)
}
