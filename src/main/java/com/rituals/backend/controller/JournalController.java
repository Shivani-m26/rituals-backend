package com.rituals.backend.controller;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.Journal;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final AppUserRepository userRepository;

    @PostMapping
    public ResponseEntity<Journal> createJournal(@RequestBody Journal journal) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        journal.setUser(user);
        if (journal.getDate() == null) {
            journal.setDate(LocalDate.now());
        }
        
        return ResponseEntity.ok(journalService.saveJournal(journal));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Journal>> getJournalHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(journalService.getJournalsByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Journal> getJournalById(@PathVariable("id") Long id) {
        return journalService.getJournalById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
