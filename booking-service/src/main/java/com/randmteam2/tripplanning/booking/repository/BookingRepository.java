package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Booking;
import com.randmteam2.tripplanning.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query(value = """
            SELECT * FROM bookings
            WHERE (:status IS NULL OR status = :status)
            AND created_at BETWEEN :startDate AND :endDate
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Booking> searchBookings(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // S5-F3: check if user exists using native SQL on users table
    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    int countUserById(@Param("userId") Long userId);

    // S5-F3: find confirmed bookings for a given user
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    // For compatibility with ongoing features
    // For S5-F1
    List<Booking> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(BookingStatus status, LocalDateTime start, LocalDateTime end); 
    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end); 
