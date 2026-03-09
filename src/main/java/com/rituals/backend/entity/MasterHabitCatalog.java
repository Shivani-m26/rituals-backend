package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "master_habits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterHabitCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private String preference;

    @Column(name = "difficulty_level", nullable = false)
    private String difficulty; // Easy, Medium, Hard

    @Column(name = "time_minutes", nullable = false)
    private Integer timeMinutes;

    @Column(name = "suitable_gender")
    private String suitableGender; // Male, Female, both

    @Column(length = 1000)
    private String description;
}
