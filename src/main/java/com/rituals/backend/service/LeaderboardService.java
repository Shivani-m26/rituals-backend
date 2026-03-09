package com.rituals.backend.service;

import com.rituals.backend.dto.LeaderboardEntry;
import com.rituals.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final AppUserRepository userRepository;

    public List<LeaderboardEntry> getLeaderboard() {
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> u2.getTotalPoints().compareTo(u1.getTotalPoints()))
                .limit(10)
                .map(user -> new LeaderboardEntry(
                        obfuscateUsername(user.getUsername()),
                        user.getTotalPoints(),
                        user.getBadges() != null ? (long) user.getBadges().size() : 0L,
                        calculateRank(user.getTotalPoints())
                ))
                .collect(Collectors.toList());
    }

    private String calculateRank(Integer points) {
        if (points >= 1000) return "Master of Rituals";
        if (points >= 500) return "Ritual Sage";
        if (points >= 200) return "Disciplined Soul";
        return "Initiate";
    }

    private String obfuscateUsername(String username) {
        if (username == null || username.length() < 3) return "Ritualist";
        return username.substring(0, 2) + "***" + username.substring(username.length() - 1);
    }
}
