package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "journals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AppUser user;

    @Column(nullable = false)
    private LocalDate date;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // Allows long words and emojis

    @Column(name = "theme_id")
    @Builder.Default
    private Integer themeId = 1; // 1 to 10 mapped on frontend

    @Column(name = "font_family")
    @Builder.Default
    private String fontFamily = "'Caveat', cursive";
}
