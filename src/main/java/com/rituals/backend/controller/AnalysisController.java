package com.rituals.backend.controller;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.MasterHabitCatalog;
import com.rituals.backend.entity.UserPreference;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.repository.UserPreferenceRepository;
import com.rituals.backend.service.HabitRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final UserPreferenceRepository preferenceRepository;
    private final HabitRecommendationService recommendationService;
    private final AppUserRepository userRepository;

    @PostMapping("/preferences")
    public ResponseEntity<?> savePreferences(@RequestBody UserPreference preference, Authentication authentication) {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        preference.setUser(user);
        
        // Update existing if present
        preferenceRepository.findByUserId(user.getId()).ifPresent(p -> preference.setId(p.getId()));
        
        UserPreference saved = preferenceRepository.save(preference);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<MasterHabitCatalog>> getRecommendations(Authentication authentication) {
        AppUser user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        UserPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Preferences not found. Please complete analysis first."));
        
        List<MasterHabitCatalog> recommendations = recommendationService.getRecommendations(preference);
        return ResponseEntity.ok(recommendations);
    }
}
