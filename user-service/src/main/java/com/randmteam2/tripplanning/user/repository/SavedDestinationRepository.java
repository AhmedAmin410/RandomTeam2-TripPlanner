package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.SavedDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SavedDestinationRepository extends JpaRepository<SavedDestination, Long> {

    List<SavedDestination> findByUser_Id(Long userId);

}