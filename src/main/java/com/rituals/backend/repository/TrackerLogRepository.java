package com.rituals.backend.repository;

import com.rituals.backend.entity.TrackerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackerLogRepository extends JpaRepository<TrackerLog, Long> {
    List<TrackerLog> findByUserHabitId(Long userHabitId);
    Optional<TrackerLog> findByUserHabitIdAndDate(Long userHabitId, LocalDate date);
    void deleteByUserHabitId(Long userHabitId);
}
