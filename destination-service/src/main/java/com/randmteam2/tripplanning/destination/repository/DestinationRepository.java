package com.randmteam2.tripplanning.destination.repository;

import com.randmteam2.tripplanning.destination.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

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

    @Query(value = """
            SELECT d.id, d.name, d.rating,
                   COALESCE((
                       SELECT COUNT(*)::bigint FROM bookings b
                       INNER JOIN itineraries i ON b.itinerary_id = i.id
                       WHERE i.destination_id = d.id AND b.status = 'CONFIRMED'
                   ), 0)
            FROM destinations d
            ORDER BY d.rating DESC NULLS LAST, d.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopRatedDestinationsReport(@Param("limit") int limit);

    @Query(value = """
            SELECT i.destination_id, i.status
            FROM itineraries i
            WHERE i.id = :itineraryId
            """, nativeQuery = true)
    List<Object[]> findItineraryDestinationIdAndStatus(@Param("itineraryId") Long itineraryId);

    @Query(value = """
            SELECT COUNT(*) FROM users u
            WHERE u.id = :userId AND u.role::text = 'ADMIN'
            """, nativeQuery = true)
    long countAdminUserById(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT d FROM Destination d
            LEFT JOIN FETCH d.destinationReviews
            WHERE d.id = :id
            """)
    Optional<Destination> findByIdWithDestinationReviews(@Param("id") Long id);
}
