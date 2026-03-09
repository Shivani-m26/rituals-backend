package com.rituals.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private String username;
    private Integer totalPoints;
    private Long badgeCount;
    private String rank; // e.g. "Ritualist", "Sage", "Master"
}
