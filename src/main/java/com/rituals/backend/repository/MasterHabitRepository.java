package com.rituals.backend.repository;

import com.rituals.backend.entity.MasterHabitCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterHabitRepository extends JpaRepository<MasterHabitCatalog, Long> {
    List<MasterHabitCatalog> findByDomainInAndSuitableGenderIn(List<String> domains, List<String> genders);
}
