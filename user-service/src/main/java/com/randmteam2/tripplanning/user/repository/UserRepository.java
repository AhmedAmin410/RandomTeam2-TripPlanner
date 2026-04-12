package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = """
        SELECT 
            COUNT(*) AS total_trips,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_trips,
            COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_trips,
            COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN estimated_budget END), 0) AS total_spent,
            COALESCE(AVG(CASE WHEN status = 'COMPLETED' THEN estimated_budget END), 0) AS avg_budget
        FROM itineraries
        WHERE user_id = :userId
    """, nativeQuery = true)
    List<Object[]> getUserTripSummary(@Param("userId") Long userId);

    @Query(value = """
        SELECT * FROM users 
        WHERE preferences ->> :key = :value
    """, nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);

    @Query(value = """
    SELECT * FROM users
    WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
    AND (:email IS NULL OR LOWER(email) LIKE LOWER(CONCAT('%', :email, '%')))
    AND (:role IS NULL OR role::text = :role)
""", nativeQuery = true)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("role") String role,
            @Param("email") String email
    );

    @Query(value = """
    SELECT 
        u.id,
        u.name,
        COALESCE(SUM(i.estimated_budget), 0) AS total_spent,
        COUNT(i.id) AS trip_count
    FROM users u
    LEFT JOIN itineraries i 
        ON u.id = i.user_id 
        AND i.status = 'COMPLETED'
    GROUP BY u.id, u.name
    ORDER BY total_spent DESC
    LIMIT :limit
""", nativeQuery = true)
    List<Object[]> getTopTravelers(
            @Param("limit") int limit
    );

    @Query(value = """
    SELECT u.*
    FROM users u
    JOIN itineraries i ON u.id = i.user_id
    WHERE u.preferences ->> 'travelStyle' = :style
      AND i.status = 'COMPLETED'
    GROUP BY u.id
    HAVING COUNT(i.id) >= :minTrips
""", nativeQuery = true)
    List<User> findUsersByTravelStyleAndMinTrips(
            @Param("style") String style,
            @Param("minTrips") int minTrips
    );

    @Query(value = """
    SELECT COUNT(*)
    FROM itineraries
    WHERE user_id = :userId
      AND status IN ('DRAFT', 'IN_PROGRESS')
""", nativeQuery = true)
    long countActiveItineraries(@Param("userId") Long userId);
}