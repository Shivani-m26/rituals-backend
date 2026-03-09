package com.rituals.backend.repository;

import com.rituals.backend.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {
    List<Journal> findAllByUserIdOrderByDateDesc(Long userId);
}
