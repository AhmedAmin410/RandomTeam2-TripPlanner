package com.randmteam2.tripplanning.booking.feign;

import com.randmteam2.tripplanning.contracts.feign.UserServiceClient;
import com.randmteam2.tripplanning.contracts.feign.ItineraryServiceClient;
import com.randmteam2.tripplanning.contracts.feign.DestinationServiceClient;
import com.randmteam2.tripplanning.contracts.dto.BatchItineraryRequest;
import com.randmteam2.tripplanning.contracts.dto.BatchDestinationRequest;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class BookingFeignClients {



    /**
     * Safe wrapper service for UserServiceClient.
     * Intercepts all FeignException subclasses and handles them gracefully.
     * Maps 404 responses to Optional.empty() and other errors to BAD_GATEWAY.
     */
    @Service
    public static class UserServiceSafeClient {

        private final UserServiceClient userServiceClient;

        public UserServiceSafeClient(UserServiceClient userServiceClient) {
            this.userServiceClient = userServiceClient;
        }

        /**
         * Safely retrieves a user by userId.
         * Returns Optional.empty() if user not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param userId the user ID
         * @return Optional containing user data, or Optional.empty() if not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public Optional<Object> getUser(Long userId) {
            try {
                Object user = userServiceClient.getUser(userId);
                return Optional.of(user);
            } catch (FeignException.NotFound e) {
                return Optional.empty();
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "User service unavailable: " + e.getMessage(),
                        e
                );
            }
        }

    }



    /**
     * Safe wrapper service for ItineraryServiceClient.
     * Intercepts all FeignException subclasses and handles them gracefully.
     * Maps 404 responses to safe defaults and other errors to BAD_GATEWAY.
     */
    @Service
    public static class ItineraryServiceSafeClient {

        private final ItineraryServiceClient itineraryServiceClient;

        public ItineraryServiceSafeClient(ItineraryServiceClient itineraryServiceClient) {
            this.itineraryServiceClient = itineraryServiceClient;
        }

        /**
         * Safely retrieves an itinerary by itineraryId.
         * Returns Optional.empty() if itinerary not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param itineraryId the itinerary ID
         * @return Optional containing itinerary data, or Optional.empty() if not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public Optional<Object> getItinerary(Long itineraryId) {
            try {
                Object itinerary = itineraryServiceClient.getItinerary(itineraryId);
                return Optional.of(itinerary);
            } catch (FeignException.NotFound e) {
                return Optional.empty();
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(),
                        e
                );
            }
        }

        /**
         * Safely retrieves the active itinerary count for a destination.
         * Returns 0 if the destination not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param destinationId the destination ID
         * @return the count of active itineraries, or 0 if destination not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public int getDestinationActiveCount(Long destinationId) {
            try {
                return itineraryServiceClient.getDestinationActiveItineraryCount(destinationId);
            } catch (FeignException.NotFound e) {
                return 0;
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(),
                        e
                );
            }
        }

        /**
         * Safely batch retrieves itineraries.
         * Returns an empty list if batch not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param request the batch request containing itinerary IDs
         * @return a list of itinerary data, or empty list if not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public List<Object> batchGetItineraries(BatchItineraryRequest request) {
            try {
                var result = itineraryServiceClient.batchGetItineraries(request);
                return result != null ? new java.util.ArrayList<>(result) : List.of();
            } catch (FeignException.NotFound e) {
                return List.of();
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Itinerary service unavailable: " + e.getMessage(),
                        e
                );
            }
        }

    }



    /**
     * Safe wrapper service for DestinationServiceClient.
     * Intercepts all FeignException subclasses and handles them gracefully.
     * Maps 404 responses to safe defaults and other errors to BAD_GATEWAY.
     */
    @Service
    public static class DestinationServiceSafeClient {

        private final DestinationServiceClient destinationServiceClient;

        public DestinationServiceSafeClient(DestinationServiceClient destinationServiceClient) {
            this.destinationServiceClient = destinationServiceClient;
        }

        /**
         * Safely retrieves a destination by ID.
         * Returns Optional.empty() if destination not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param destinationId the destination ID
         * @return Optional containing destination data, or Optional.empty() if not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public Optional<Object> getDestination(Long destinationId) {
            try {
                Object destination = destinationServiceClient.getDestination(destinationId);
                return Optional.of(destination);
            } catch (FeignException.NotFound e) {
                return Optional.empty();
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Destination service unavailable: " + e.getMessage(),
                        e
                );
            }
        }

        /**
         * Safely batch retrieves destinations.
         * Returns an empty list if batch not found (404).
         * Throws ResponseStatusException with BAD_GATEWAY status for other errors.
         *
         * @param request the batch request containing destination IDs
         * @return a list of destination data, or empty list if not found
         * @throws ResponseStatusException if a non-404 FeignException occurs
         */
        public List<Object> batchGetDestinations(BatchDestinationRequest request) {
            try {
                var result = destinationServiceClient.batchGetDestinations(request);
                return result != null ? new java.util.ArrayList<>(result) : List.of();
            } catch (FeignException.NotFound e) {
                return List.of();
            } catch (FeignException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Destination service unavailable: " + e.getMessage(),
                        e
                );
            }
        }
    }

}

