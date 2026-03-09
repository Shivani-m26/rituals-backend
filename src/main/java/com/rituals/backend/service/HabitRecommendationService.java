package com.rituals.backend.service;

import com.rituals.backend.entity.MasterHabitCatalog;
import com.rituals.backend.entity.UserPreference;
import com.rituals.backend.repository.MasterHabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabitRecommendationService {

    private final MasterHabitRepository habitRepository;

    public List<MasterHabitCatalog> getRecommendations(UserPreference preference) {
        List<MasterHabitCatalog> allHabits = habitRepository.findAll();
        System.out.println("[DEBUG] Recommendation Request. Total Catalog: " + allHabits.size());

        String domain = preference.getSelectedDomain();
        String gender = preference.getGender();
        String targetDiff = preference.getDifficulty();
        Integer targetTime = preference.getTimeMinutes();
        
        List<String> userPrefs = Arrays.asList(preference.getSelectedPreferences().split(","));

        // Stage 1-3: Try each preference in order
        for (String pref : userPrefs) {
            System.out.println("[DEBUG] Checking Preference: " + pref);
            
            // 1. Exact Match
            List<MasterHabitCatalog> matches = allHabits.stream()
                .filter(h -> isGenderMatch(h, gender))
                .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                .filter(h -> h.getPreference().equalsIgnoreCase(pref.trim()))
                .filter(h -> h.getDifficulty().equalsIgnoreCase(targetDiff))
                .filter(h -> h.getTimeMinutes().equals(targetTime))
                .limit(5)
                .collect(Collectors.toList());
            if (!matches.isEmpty()) return matches;

            // 2. Closest Time Match
            matches = allHabits.stream()
                .filter(h -> isGenderMatch(h, gender))
                .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                .filter(h -> h.getPreference().equalsIgnoreCase(pref.trim()))
                .filter(h -> h.getDifficulty().equalsIgnoreCase(targetDiff))
                .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - targetTime)))
                .limit(5)
                .collect(Collectors.toList());
            if (!matches.isEmpty()) return matches;

            // 3. Difficulty Fallback (Hard -> Medium -> Easy)
            List<String> diffs = getDiffFallback(targetDiff);
            for (String d : diffs) {
                matches = allHabits.stream()
                    .filter(h -> isGenderMatch(h, gender))
                    .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                    .filter(h -> h.getPreference().equalsIgnoreCase(pref.trim()))
                    .filter(h -> h.getDifficulty().equalsIgnoreCase(d))
                    .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - targetTime)))
                    .limit(5)
                    .collect(Collectors.toList());
                if (!matches.isEmpty()) return matches;
            }
        }

        // 4. Same Domain Fallback
        System.out.println("[DEBUG] All preferences failed. Falling back to Domain: " + domain);
        return allHabits.stream()
            .filter(h -> isGenderMatch(h, gender))
            .filter(h -> h.getDomain().equalsIgnoreCase(domain))
            .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - targetTime)))
            .limit(5)
            .collect(Collectors.toList());
    }

    private boolean isGenderMatch(MasterHabitCatalog habit, String userGender) {
        String suit = habit.getSuitableGender();
        if (suit == null || suit.equalsIgnoreCase("both")) return true;
        return suit.equalsIgnoreCase(userGender);
    }

    private List<String> getDiffFallback(String current) {
        if (current.equalsIgnoreCase("Hard")) return Arrays.asList("Medium", "Easy");
        if (current.equalsIgnoreCase("Medium")) return Arrays.asList("Easy");
        return Collections.emptyList();
    }
}
