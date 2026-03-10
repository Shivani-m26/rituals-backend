package com.rituals.backend.repository;

import com.rituals.backend.entity.UserHabit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHabitRepository extends JpaRepository<UserHabit, Long> {
    List<UserHabit> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT h FROM UserHabit h JOIN FETCH h.user WHERE h.isActive = true")
    List<UserHabit> findAllActiveWithUser();
}
