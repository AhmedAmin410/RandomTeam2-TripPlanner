package com.randmteam2.tripplanning.activity.repository;

import com.randmteam2.tripplanning.activity.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query(value = "SELECT * FROM activities WHERE metadata->>:key = :value", nativeQuery = true)
    List<Activity> findByMetadataEq(@Param("key") String key, @Param("value") String value);

    @Query(value = "SELECT * FROM activities WHERE CAST(metadata->>:key AS numeric) > CAST(:value AS numeric)", nativeQuery = true)
    List<Activity> findByMetadataGt(@Param("key") String key, @Param("value") String value);

    @Query(value = "SELECT * FROM activities WHERE CAST(metadata->>:key AS numeric) < CAST(:value AS numeric)", nativeQuery = true)
    List<Activity> findByMetadataLt(@Param("key") String key, @Param("value") String value);

    @Modifying
    @Query(value = "DELETE FROM activities WHERE scheduled_time < :cutoff", nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    List<Activity> findByItineraryId(Long itineraryId);

    @Query(value = "SELECT * FROM activities WHERE itinerary_id = :itineraryId ORDER BY scheduled_time DESC LIMIT 1", nativeQuery = true)
    Activity findLatestByItineraryId(@Param("itineraryId") Long itineraryId);
    // S4-F6: Handles optional category and sorts by time
    @Query(value = """
    SELECT * FROM activities 
    WHERE DATE(scheduled_time) BETWEEN :start AND :end
    AND (:category IS NULL OR CAST(category AS VARCHAR) = :category)
    ORDER BY scheduled_time ASC
    """, nativeQuery = true)
    List<Activity> findByHistory(
            @Param("start") java.time.LocalDate start,
            @Param("end") java.time.LocalDate end,
            @Param("category") String category
    );

    // Requirement: Used to trigger the 404 if the itinerary doesn't exist
    boolean existsByItineraryId(Long itineraryId);

    // Requirement: Native SQL with JSONB casting for AVG/MAX/MIN calculations
    @Query(value = "SELECT COUNT(*), " +
            "AVG(CAST(metadata->>'cost' AS numeric)), " +
            "MAX(CAST(metadata->>'cost' AS numeric)), " +
            "MIN(scheduled_time), " +
            "MAX(scheduled_time) " +
            "FROM activities " +
            "WHERE itinerary_id = :itineraryId " +
            "AND scheduled_time BETWEEN :start AND :end",
            nativeQuery = true)
    Object getActivitySummaryRaw(
            @Param("itineraryId") Long itineraryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    // S4-F9: Native SQL for JSON metadata and time interval
    @Query(value = "SELECT * FROM activities a " +
            "WHERE CAST(a.metadata->>'cost' AS numeric) <= :maxCost " +
            "AND a.scheduled_time >= NOW() - (:since || ' minutes')::interval",
            nativeQuery = true)
    List<Activity> findBudgetFriendly(
            @Param("maxCost") Double maxCost,
            @Param("since") Integer sinceMinutes
    );

    @Query(value = "SELECT COUNT(*) FROM itineraries WHERE id = :itineraryId", nativeQuery = true)
    Integer checkItineraryExists(@Param("itineraryId") Long itineraryId);
}