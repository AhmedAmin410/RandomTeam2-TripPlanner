package com.randmteam2.tripplanning.user.repository;

import java.util.List;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = """
    SELECT * FROM users 
    WHERE preferences ->> :key = :value
""", nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);
}