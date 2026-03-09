package com.rituals.backend.service;

import com.rituals.backend.entity.Journal;
import com.rituals.backend.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;

    public Journal saveJournal(Journal journal) {
        return journalRepository.save(journal);
    }

    public List<Journal> getJournalsByUserId(Long userId) {
        return journalRepository.findAllByUserIdOrderByDateDesc(userId);
    }

    public Optional<Journal> getJournalById(Long id) {
        return journalRepository.findById(id);
    }
}
