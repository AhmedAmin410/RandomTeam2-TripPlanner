package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.SavedDestination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedDestinationRepository extends JpaRepository<SavedDestination, Long> {
}