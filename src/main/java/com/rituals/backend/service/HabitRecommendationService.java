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
        
        System.out.println("[DEBUG] Domain: " + domain + ", Gender: " + gender + ", Difficulty: " + targetDiff + ", Time: " + targetTime);
        
        String prefString = preference.getSelectedPreferences();
        if (prefString == null || prefString.isBlank()) {
            System.out.println("[DEBUG] No preferences selected, falling back to domain-only search.");
            return domainFallback(allHabits, domain, gender, targetTime);
        }

        List<String> userPrefs = Arrays.stream(prefString.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        System.out.println("[DEBUG] User Preferences: " + userPrefs);

        // Stage 1: Try exact match for each preference
        for (String pref : userPrefs) {
            System.out.println("[DEBUG] Checking Preference: " + pref);
            
            // 1. Exact Match (domain + pref + difficulty + time)
            List<MasterHabitCatalog> matches = allHabits.stream()
                .filter(h -> isGenderMatch(h, gender))
                .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                .filter(h -> h.getPreference().equalsIgnoreCase(pref))
                .filter(h -> h.getDifficulty().equalsIgnoreCase(targetDiff))
                .filter(h -> targetTime == null || h.getTimeMinutes().equals(targetTime))
                .limit(5)
                .collect(Collectors.toList());
            if (!matches.isEmpty()) {
                System.out.println("[DEBUG] Stage 1 (Exact): Found " + matches.size() + " matches for pref: " + pref);
                return matches;
            }

            // 2. Closest Time Match (domain + pref + difficulty, sorted by closest time)
            matches = allHabits.stream()
                .filter(h -> isGenderMatch(h, gender))
                .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                .filter(h -> h.getPreference().equalsIgnoreCase(pref))
                .filter(h -> h.getDifficulty().equalsIgnoreCase(targetDiff))
                .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - (targetTime != null ? targetTime : 30))))
                .limit(5)
                .collect(Collectors.toList());
            if (!matches.isEmpty()) {
                System.out.println("[DEBUG] Stage 2 (Closest Time): Found " + matches.size() + " matches for pref: " + pref);
                return matches;
            }

            // 3. Difficulty Fallback (Hard -> Medium -> Easy)
            List<String> diffs = getDiffFallback(targetDiff);
            for (String d : diffs) {
                matches = allHabits.stream()
                    .filter(h -> isGenderMatch(h, gender))
                    .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                    .filter(h -> h.getPreference().equalsIgnoreCase(pref))
                    .filter(h -> h.getDifficulty().equalsIgnoreCase(d))
                    .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - (targetTime != null ? targetTime : 30))))
                    .limit(5)
                    .collect(Collectors.toList());
                if (!matches.isEmpty()) {
                    System.out.println("[DEBUG] Stage 3 (Difficulty Fallback " + d + "): Found " + matches.size() + " matches for pref: " + pref);
                    return matches;
                }
            }

            // 4. Domain + Preference only (any difficulty, any time)
            matches = allHabits.stream()
                .filter(h -> isGenderMatch(h, gender))
                .filter(h -> h.getDomain().equalsIgnoreCase(domain))
                .filter(h -> h.getPreference().equalsIgnoreCase(pref))
                .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - (targetTime != null ? targetTime : 30))))
                .limit(5)
                .collect(Collectors.toList());
            if (!matches.isEmpty()) {
                System.out.println("[DEBUG] Stage 4 (Domain+Pref only): Found " + matches.size() + " matches for pref: " + pref);
                return matches;
            }
        }

        // Stage 5: Same Domain Fallback (any preference within the domain)
        System.out.println("[DEBUG] All preferences failed. Falling back to Domain: " + domain);
        List<MasterHabitCatalog> domainResults = domainFallback(allHabits, domain, gender, targetTime);
        if (!domainResults.isEmpty()) return domainResults;

        // Stage 6: Global Fallback — ALWAYS return something
        System.out.println("[DEBUG] Domain fallback also empty. Returning global top habits.");
        return allHabits.stream()
            .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - (targetTime != null ? targetTime : 30))))
            .limit(5)
            .collect(Collectors.toList());
    }

    private List<MasterHabitCatalog> domainFallback(List<MasterHabitCatalog> allHabits, String domain, String gender, Integer targetTime) {
        return allHabits.stream()
            .filter(h -> isGenderMatch(h, gender))
            .filter(h -> h.getDomain().equalsIgnoreCase(domain))
            .sorted(Comparator.comparingInt(h -> Math.abs(h.getTimeMinutes() - (targetTime != null ? targetTime : 30))))
            .limit(5)
            .collect(Collectors.toList());
    }

    private boolean isGenderMatch(MasterHabitCatalog habit, String userGender) {
        String suit = habit.getSuitableGender();
        if (suit == null || suit.equalsIgnoreCase("both")) return true;
        if (userGender == null || userGender.isBlank()) return true;
        return suit.equalsIgnoreCase(userGender);
    }

    private List<String> getDiffFallback(String current) {
        if (current == null) return Arrays.asList("Easy", "Medium", "Hard");
        if (current.equalsIgnoreCase("Hard")) return Arrays.asList("Medium", "Easy");
        if (current.equalsIgnoreCase("Medium")) return Arrays.asList("Easy", "Hard");
        if (current.equalsIgnoreCase("Easy")) return Arrays.asList("Medium", "Hard");
        return Arrays.asList("Easy", "Medium", "Hard");
    }
}
