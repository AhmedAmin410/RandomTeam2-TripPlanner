package com.randomteam2.tripplanning.destination.repository;

import com.randomteam2.tripplanning.destination.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@RepositoryRestResource(exported = false)
public interface DestinationRepository extends JpaRepository<Destination, Long> {

    @Deprecated
    default long countActiveItinerariesReferencingDestination(Long destinationId) {
        throw new UnsupportedOperationException("Use itinerary-service via Feign");
    }

    @Deprecated
    default List<Object[]> findTopRatedDestinationsReport(int limit) {
        throw new UnsupportedOperationException("Use destination-service plus itinerary-service aggregates");
    }

    @Deprecated
    default List<Object[]> findItineraryDestinationIdAndStatus(Long itineraryId) {
        throw new UnsupportedOperationException("Use itinerary-service via Feign");
    }

    @Deprecated
    default long countAdminUserById(Long userId) {
        throw new UnsupportedOperationException("Use user-service via Feign");
    }

    @Deprecated
    default Object[] findDestinationRevenueSummary(Long destinationId, LocalDate startDate, LocalDate endDate) {
        throw new UnsupportedOperationException("Use itinerary-service via Feign");
    }

    @Deprecated
    default Object[] findDestinationDashboardStats(Long destinationId) {
        throw new UnsupportedOperationException("Use itinerary-service via Feign");
    }

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
