package com.randmteam2.tripplanning.itinerary.security;

import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;

public class UserLoaderHandler extends AuthHandler {

    private final ItineraryRepository itineraryRepository;

    public UserLoaderHandler(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        boolean exists = itineraryRepository.existsByUserEmail(ctx.getEmail());
        if (!exists) {
            throw new AuthException("User not found", 401);
        }
        if (next != null) next.handle(ctx);
    }
}