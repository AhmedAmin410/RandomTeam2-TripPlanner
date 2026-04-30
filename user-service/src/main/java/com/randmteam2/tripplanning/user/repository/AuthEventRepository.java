package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.AuthEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
    List<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId);
}
