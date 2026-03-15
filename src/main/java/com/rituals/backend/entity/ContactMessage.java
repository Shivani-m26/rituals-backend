package com.rituals.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comments;

    @Column(name = "submitted_at")
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}
