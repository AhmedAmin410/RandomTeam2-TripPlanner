package com.randmteam2.tripplanning.activity.repository;

import com.randmteam2.tripplanning.activity.model.ActivityLifecycleEvent;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLifecycleEventRepository extends CassandraRepository<ActivityLifecycleEvent, Long> {

    @Query("SELECT * FROM activity_lifecycle_events WHERE activity_id = ?0")
    List<ActivityLifecycleEvent> findByActivityId(Long activityId);

    @Query("SELECT * FROM activity_lifecycle_events WHERE activity_id = ?0 AND timestamp >= ?1 AND timestamp <= ?2")
    List<ActivityLifecycleEvent> findByActivityIdAndTimestampBetween(Long activityId, Instant startTime, Instant endTime);
}
