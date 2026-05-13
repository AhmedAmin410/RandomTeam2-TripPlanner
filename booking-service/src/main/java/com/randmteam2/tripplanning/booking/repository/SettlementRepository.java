package com.randmteam2.tripplanning.booking.repository;

import com.randmteam2.tripplanning.booking.model.Settlement;
import com.randmteam2.tripplanning.booking.model.SettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByItineraryId(Long itineraryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Settlement s where s.itineraryId = :itineraryId")
    Optional<Settlement> lockByItineraryId(@Param("itineraryId") Long itineraryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Settlement s
            set s.status = :target
            where s.id = :id and s.status = :source
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("source") SettlementStatus source,
                         @Param("target") SettlementStatus target);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Settlement s
            set s.status = :target,
                s.settledAt = :settledAt,
                s.failureReason = :failureReason
            where s.id = :id and s.status = :source
            """)
    int finishSettlement(@Param("id") Long id,
                         @Param("source") SettlementStatus source,
                         @Param("target") SettlementStatus target,
                         @Param("settledAt") LocalDateTime settledAt,
                         @Param("failureReason") String failureReason);
}
