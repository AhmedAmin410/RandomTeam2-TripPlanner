package com.randmteam2.tripplanning.booking.feign;

import com.randmteam2.tripplanning.contracts.dto.BatchDestinationRequest;
import com.randmteam2.tripplanning.contracts.dto.BatchItineraryRequest;
import com.randmteam2.tripplanning.contracts.dto.DestinationSummaryDTO;
import com.randmteam2.tripplanning.contracts.dto.ItinerarySummaryDTO;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BookingFeignClients {

    // ── UserServiceSafeClient ─────────────────────────────────────────────
    @Service
    public static class UserServiceSafeClient {

        private static final Logger log = LoggerFactory.getLogger(UserServiceSafeClient.class);

        private final UserServiceClient userServiceClient;

        public UserServiceSafeClient(UserServiceClient userServiceClient) {
            this.userServiceClient = userServiceClient;
        }

        /**
         * Returns Optional.empty() if user not found (404).
         * Throws BAD_GATEWAY for any other Feign error.
         */
        public Optional<Map<String, Object>> getUser(Long userId) {
            try {
                log.info("Calling user-service.getUser with args={}", userId);
                Map<String, Object> user = userServiceClient.getUser(userId);
                log.info("user-service.getUser returned successfully");
                return Optional.ofNullable(user);
            } catch (FeignException.NotFound e) {
                return Optional.empty();
            } catch (FeignException e) {
                log.warn("Feign call to user-service failed for userId={}: {}", userId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "User service unavailable: " + e.getMessage(), e);
            }
        }
    }

    // ── ItineraryServiceSafeClient ────────────────────────────────────────
    @Service
    public static class ItineraryServiceSafeClient {

        private static final Logger log = LoggerFactory.getLogger(ItineraryServiceSafeClient.class);

        private final ItineraryServiceClient itineraryServiceClient;

        public ItineraryServiceSafeClient(ItineraryServiceClient itineraryServiceClient) {
            this.itineraryServiceClient = itineraryServiceClient;
        }

        /**
         * Returns Optional.empty() if itinerary not found (404).
         * Throws BAD_GATEWAY for any other Feign error.
         */
        public Optional<Map<String, Object>> getItinerary(Long itineraryId) {
            try {
                log.info("Calling itinerary-service.getItinerary with args={}", itineraryId);
                Map<String, Object> itinerary = itineraryServiceClient.getItinerary(itineraryId);
                log.info("itinerary-service.getItinerary returned successfully");
                return Optional.ofNullable(itinerary);
            } catch (FeignException.NotFound e) {
                return Optional.empty();
            } catch (FeignException e) {
                log.warn("Feign call to itinerary-service failed for itineraryId={}: {}", itineraryId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(), e);
            }
        }

        /**
         * Returns 0 if destination not found (404).
         * Throws BAD_GATEWAY for any other Feign error.
         */
        public int getDestinationActiveCount(Long destinationId) {
            try {
                log.info("Calling itinerary-service.getDestinationActiveItineraryCount with args={}", destinationId);
                int count = itineraryServiceClient.getDestinationActiveItineraryCount(destinationId);
                log.info("itinerary-service.getDestinationActiveItineraryCount returned successfully");
                return count;
            } catch (FeignException.NotFound e) {
                return 0;
            } catch (FeignException e) {
                log.warn("Feign call to itinerary-service (active-count) failed for destinationId={}: {}",
                        destinationId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(), e);
            }
        }

        /**
         * Batch fetch itinerary summaries. Returns empty list on 404.
         */
        public List<ItinerarySummaryDTO> batchGetItineraries(List<Long> itineraryIds) {
            try {
                log.info("Calling itinerary-service.batchGetItineraries with args={}", itineraryIds);
                List<ItinerarySummaryDTO> result = itineraryServiceClient.batchGetItineraries(
                        new BatchItineraryRequest(itineraryIds));
                log.info("itinerary-service.batchGetItineraries returned successfully");
                return result != null ? result : List.of();
            } catch (FeignException.NotFound e) {
                return List.of();
            } catch (FeignException e) {
                log.warn("Feign batch call to itinerary-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(), e);
            }
        }
    }

    // ── DestinationServiceSafeClient ──────────────────────────────────────
    @Service
    public static class DestinationServiceSafeClient {

        private static final Logger log = LoggerFactory.getLogger(DestinationServiceSafeClient.class);

        private final DestinationServiceClient destinationServiceClient;

        public DestinationServiceSafeClient(DestinationServiceClient destinationServiceClient) {
            this.destinationServiceClient = destinationServiceClient;
        }

        /**
         * Batch fetch destination summaries. Returns empty list on 404.
         */
        public List<DestinationSummaryDTO> batchGetDestinations(List<Long> destinationIds) {
            try {
                log.info("Calling destination-service.batchGetDestinations with args={}", destinationIds);
                List<DestinationSummaryDTO> result = destinationServiceClient.batchGetDestinations(
                        new BatchDestinationRequest(destinationIds));
                log.info("destination-service.batchGetDestinations returned successfully");
                return result != null ? result : List.of();
            } catch (FeignException.NotFound e) {
                return List.of();
            } catch (FeignException e) {
                log.warn("Feign batch call to destination-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Destination service unavailable: " + e.getMessage(), e);
            }
        }
    }
}