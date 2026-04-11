package com.randmteam2.tripplanning.destination.repository;

import com.randmteam2.tripplanning.destination.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface DestinationRepository extends JpaRepository<Destination, Long> {

    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE destination_id = :destinationId
            AND status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS')
            """, nativeQuery = true)
    long countActiveItinerariesReferencingDestination(@Param("destinationId") Long destinationId);

    @Query(value = """
            SELECT * FROM destinations d
            WHERE d.details IS NOT NULL
            AND d.details ->> :key = :value
            AND (:statusFilter IS NULL OR d.status = :statusFilter)
            """, nativeQuery = true)
    List<Destination> searchByDetailsKeyValue(
            @Param("key") String key,
            @Param("value") String value,
            @Param("statusFilter") String statusFilter);
}
