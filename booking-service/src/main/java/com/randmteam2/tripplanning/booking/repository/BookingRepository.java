package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query(value = "SELECT status FROM itineraries WHERE id = :id", nativeQuery = true)
    Optional<String> findItineraryStatusById(@Param("id") Long id);

    @Query(value = "SELECT user_id FROM itineraries WHERE id = :id", nativeQuery = true)
    Optional<Long> findUserIdByItineraryId(@Param("id") Long id);
    
    // For S5-F1
    List<Booking> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(BookingStatus status, LocalDateTime start, LocalDateTime end); 
    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end); 
    
    // For S5-F3
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
}
