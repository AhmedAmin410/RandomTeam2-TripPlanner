package com.randmteam2.tripplanning.destination.repository;

import com.randmteam2.tripplanning.destination.model.DestinationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface DestinationReviewRepository extends JpaRepository<DestinationReview, Long> {
}
