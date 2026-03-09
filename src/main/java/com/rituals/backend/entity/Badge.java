package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "badges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tier; // SILVER, GOLD, PLATINUM

    @Builder.Default
    private Boolean isPrime = false;

    @Column(name = "earned_at", nullable = false)
    private LocalDate earnedAt;

    @Column(name = "habit_reference_name")
    private String habitReferenceName; // e.g. "Completed Box Breathing 21 Days"
}
