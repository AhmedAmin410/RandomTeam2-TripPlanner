package com.randmteam2.tripplanning.activity.repository;

import com.randmteam2.tripplanning.activity.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
}