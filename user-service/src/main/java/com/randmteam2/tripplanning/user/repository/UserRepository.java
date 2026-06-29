package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

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

}
