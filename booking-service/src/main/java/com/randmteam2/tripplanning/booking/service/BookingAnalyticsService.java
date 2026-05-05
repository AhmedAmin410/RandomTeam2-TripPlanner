package com.randmteam2.tripplanning.booking.service;

import com.randmteam2.tripplanning.booking.dto.DestinationSeasonRevenueDTO;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEvent;
import com.randmteam2.tripplanning.booking.mongo.PaymentAuditEventRepository;
import com.randmteam2.tripplanning.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BookingAnalyticsService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentAuditEventRepository auditRepository;

    @Cacheable(value = "booking-service", key = "'S5-F10::' + #startDate + '::' + #endDate")
    public List<DestinationSeasonRevenueDTO> getRevenueByDestinationAndSeason(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate");
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.atTime(23, 59, 59, 999_000_000);

        List<Object[]> rows = bookingRepository.findRevenueByDestinationAndSeason(from, to);

        return rows.stream().map(r -> DestinationSeasonRevenueDTO.builder()
                .destinationId(      ((Number) r[0]).longValue()   )
                .destinationName(    (String)  r[1]                )
                .totalRevenue(       ((Number) r[2]).doubleValue()  )
                .surchargeRevenue(   ((Number) r[3]).doubleValue()  )
                .baseRevenue(        ((Number) r[4]).doubleValue()  )
                .peakBookingCount(   ((Number) r[5]).longValue()    )
                .offPeakBookingCount(((Number) r[6]).longValue()    )
                .build()
        ).toList();
    }

    // called OUTSIDE @Cacheable so it fires on every request including cache hits
    public void logAnalyticsViewed() {
        try {
            PaymentAuditEvent ev = new PaymentAuditEvent();
            ev.setAction("ANALYTICS_VIEWED");
            ev.setTimestamp(LocalDateTime.now());
            ev.setDetails(Map.of("endpoint", "S5-F10"));
            auditRepository.save(ev);
        } catch (Exception e) {
            System.err.println("[WARN] MongoDB analytics log failed: " + e.getMessage());
        }
    }
}