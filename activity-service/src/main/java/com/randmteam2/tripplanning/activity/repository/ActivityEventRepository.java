package com.randmteam2.tripplanning.activity.repository;
import com.randmteam2.tripplanning.activity.event.ActivityEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {}
