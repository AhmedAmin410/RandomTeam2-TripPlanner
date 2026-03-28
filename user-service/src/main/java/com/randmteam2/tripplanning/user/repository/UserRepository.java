package com.randmteam2.tripplanning.user.repository;

import java.util.List;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByRole(Role role);

    List<User> findByNameContainingIgnoreCaseAndRole(String name, Role role);

    List<User> findByEmailContainingIgnoreCase(String email);

    List<User> findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(String name, String email);

    List<User> findByRoleAndEmailContainingIgnoreCase(Role role, String email);

    List<User> findByNameContainingIgnoreCaseAndRoleAndEmailContainingIgnoreCase(
            String name, Role role, String email
    );

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
}