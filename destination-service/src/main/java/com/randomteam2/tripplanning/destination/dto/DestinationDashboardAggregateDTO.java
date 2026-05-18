package com.randomteam2.tripplanning.destination.dto;

/**
 * Aggregate returned by itinerary-service at
 * GET /api/itineraries/destination/{destinationId}/dashboard-aggregate
 *
 * totalItineraries    – all itineraries that reference this destination
 * completedItineraries – those whose status belongs to STATUS_COMPLETED_FAMILY (e.g. PAID)
 * totalVisitors       – count of distinct userId across all itineraries for this destination
 */
public record DestinationDashboardAggregateDTO(
        Long totalItineraries,
        Long completedItineraries,
        Long totalVisitors
) {}