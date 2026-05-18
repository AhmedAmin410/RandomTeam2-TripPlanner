package com.randomteam2.tripplanning.destination.feign;

import com.randomteam2.tripplanning.destination.dto.DestinationBookingRevenueAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.DestinationDashboardAggregateDTO;
import com.randomteam2.tripplanning.destination.dto.ItineraryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "itinerary-service", url = "${feign.itinerary-service.url}")
public interface ItineraryServiceClient {

    /**
     * S2-F3 (M3): Replaces the M1 3-table JOIN (destinations ⋈ itineraries ⋈ bookings).
     * itinerary-service encapsulates the chain internally:
     *   - local query for itinerary IDs where destination_id = ?
     *   - Feign → booking-service POST /api/bookings/aggregate-by-itineraries
     * destination-service issues exactly one outbound Feign call; no JDBC to itinerary-postgres
     * or booking-postgres.
     */
    @GetMapping("/api/itineraries/destination/{destinationId}/booking-revenue")
    DestinationBookingRevenueAggregateDTO getDestinationBookingRevenue(
            @PathVariable Long destinationId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    /**
     * S2-F4 (M3): Replaces the M1 direct SQL COUNT on the shared database.
     * Returns the number of itineraries for this destination whose status is in
     * the active set: DRAFT, PLANNED, IN_PROGRESS, COMPLETING, PAYMENT_PENDING.
     * The M3 saga states (COMPLETING, PAYMENT_PENDING) are included so a destination
     * cannot be deactivated while any trip referencing it is mid-saga.
     * Only called when transitioning to INACTIVE; ACTIVE/SEASONAL skip this entirely.
     */
    @GetMapping("/api/itineraries/destination/{destinationId}/active-count")
    Integer getDestinationActiveItineraryCount(@PathVariable Long destinationId);

    @GetMapping("/api/itineraries/{itineraryId}")
    ItineraryDTO getItinerary(@PathVariable Long itineraryId);

    /**
     * S2-F12 (M3): Fetches aggregated itinerary stats for a destination.
     * Endpoint exposed by itinerary-service:
     *   GET /api/itineraries/destination/{destinationId}/dashboard-aggregate
     *
     * Returns totalItineraries, completedItineraries (STATUS_COMPLETED_FAMILY),
     * and totalVisitors (distinct userId).
     */
    @GetMapping("/api/itineraries/destination/{destinationId}/dashboard-aggregate")
    DestinationDashboardAggregateDTO getDestinationDashboardAggregate(@PathVariable Long destinationId);
}