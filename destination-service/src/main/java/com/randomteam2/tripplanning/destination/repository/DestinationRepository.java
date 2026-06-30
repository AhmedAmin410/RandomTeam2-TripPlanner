package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface DestinationRepository extends JpaRepository<Destination, Long> {

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
            SELECT d.id, d.name, d.rating, 0::bigint
            FROM destinations d
            ORDER BY d.rating DESC NULLS LAST, d.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopRatedDestinationsReport(@Param("limit") int limit);

    @Query(value = """
            SELECT d.id, d.name, d.rating, d.total_ratings
            FROM destinations d
            WHERE d.id = :id
            """, nativeQuery = true)
    List<Object[]> findDashboardRowById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT d FROM Destination d
            LEFT JOIN FETCH d.destinationReviews
            WHERE d.id = :id
            """)
    Optional<Destination> findByIdWithDestinationReviews(@Param("id") Long id);

    @Query(value = """
            SELECT d.* FROM destinations d
            WHERE (:category IS NULL OR d.category = :category)
            AND d.rating >= :minRating AND d.rating <= :maxRating
            ORDER BY d.rating DESC
            """, nativeQuery = true)
    List<Destination> searchByCategoryAndRatingRange(
            @Param("category") String category,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating);
}
