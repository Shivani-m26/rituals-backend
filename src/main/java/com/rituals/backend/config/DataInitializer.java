package com.rituals.backend.config;

import com.rituals.backend.entity.MasterHabitCatalog;
import com.rituals.backend.repository.MasterHabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MasterHabitRepository habitRepository;

    @Override
    public void run(String... args) {
        // Force re-seed for the precision hierarchy
        if (habitRepository.count() != 600) { 
            System.out.println("[DEBUG] Seed mismatch. Re-seeding 600 habits...");
            habitRepository.deleteAll();
            seedHabits();
        }
    }

    private void seedHabits() {
        String[] domains = {
            "Productivity", "Technology", "Art & Creativity", 
            "Fitness & Health", "Self Care & Mindfulness", "Learning & Growth"
        };

        for (String domain : domains) {
            String[] preferences = getPreferences(domain);
            for (String pref : preferences) {
                String[] habits = getHabitNames(pref);
                for (int i = 0; i < habits.length; i++) {
                    String name = habits[i];
                    String diff = (i < 3) ? "Easy" : (i < 7) ? "Medium" : "Hard";
                    int time = (i % 5 == 0) ? 15 : (i % 5 == 1) ? 30 : (i % 5 == 2) ? 45 : (i % 5 == 3) ? 60 : 120;
                    String gender = (i % 3 == 0) ? "Male" : (i % 3 == 1) ? "Female" : "both";
                    
                    habitRepository.save(MasterHabitCatalog.builder()
                        .name(name + " (" + pref + ")")
                        .domain(domain)
                        .preference(pref)
                        .difficulty(diff)
                        .timeMinutes(time)
                        .suitableGender(gender)
                        .description("Master " + pref + " in " + domain + " with this ritual.")
                        .build());
                }
            }
        }
    }

    private String[] getPreferences(String domain) {
        switch (domain) {
            case "Technology": return new String[]{"Programming", "Web Development", "Testing", "Debugging", "System Design", "DevOps", "Automation", "Code Optimization", "Open Source Contribution", "Technical Writing"};
            case "Productivity": return new String[]{"Deep Work", "Inbox Zero", "Task Management", "Pomodoro", "Calendar Audit", "Goal Setting", "Review Sessions", "Delegate Tasks", "Focus Blocks", "Note Taking"};
            case "Art & Creativity": return new String[]{"Sketching", "Painting", "Digital Art", "Writing", "Music Composition", "Photography", "Crafting", "Fashion Design", "Video Editing", "UI Design"};
            case "Fitness & Health": return new String[]{"HIIT", "Yoga", "Cardio", "Strength Training", "Pilates", "Swimming", "Cycling", "Stretching", "Hydration", "Meal Planning"};
            case "Self Care & Mindfulness": return new String[]{"Meditation", "Journaling", "Breathing Exercises", "Gratitude", "Sleep Hygiene", "Digital Detox", "Skincare", "Relaxing Bath", "Nature Walk", "Affirmations"};
            case "Learning & Growth": return new String[]{"Reading", "Language Learning", "Online Courses", "Skill Practice", "Public Speaking", "Memory Training", "Networking", "Mentorship", "Financial Literacy", "Speed Reading"};
            default: return new String[0];
        }
    }

    private String[] getHabitNames(String pref) {
        if (pref.equals("Programming")) {
            return new String[]{"Solve 2 DSA problems", "Write 1 SQL query", "Build a small Java feature", "Debug a program", "Implement a simple API", "Practice recursion problems", "Write unit tests", "Refactor existing code", "Study a new algorithm", "Review open-source code"};
        }
        // Generic templates for others (10 each)
        return new String[]{
            "Quick " + pref + " session",
            "Morning " + pref + " routine",
            "Advanced " + pref + " practice",
            "Community " + pref + " task",
            "Deep " + pref + " focus",
            "Review " + pref + " basics",
            "Creative " + pref + " project",
            "Social " + pref + " ritual",
            "Nightly " + pref + " wrap-up",
            "Weekend " + pref + " challenge"
        };
    }
}
