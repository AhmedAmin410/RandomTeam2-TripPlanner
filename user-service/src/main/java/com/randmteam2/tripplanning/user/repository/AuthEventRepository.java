package com.randmteam2.tripplanning.user.repository;

import com.randmteam2.tripplanning.user.model.AuthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
    List<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId);
    Page<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}
