package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.model.DestinationReview;
import com.randomteam2.tripplanning.destination.model.DestinationReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface DestinationReviewRepository extends JpaRepository<DestinationReview, Long> {

    List<DestinationReview> findByDestination_Id(Long destinationId);

    List<DestinationReview> findByRating(Integer rating);

    List<DestinationReview> findByType(DestinationReviewType type);

    @Query("""
            SELECT r FROM DestinationReview r
            JOIN FETCH r.destination d
            WHERE r.rating <= :maxRating
            ORDER BY d.id ASC, r.id ASC
            """)
    List<DestinationReview> findLowRatedReviewsWithDestination(@Param("maxRating") int maxRating);
}
