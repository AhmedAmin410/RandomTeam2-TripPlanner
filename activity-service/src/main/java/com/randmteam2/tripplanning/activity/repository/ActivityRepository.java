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
}