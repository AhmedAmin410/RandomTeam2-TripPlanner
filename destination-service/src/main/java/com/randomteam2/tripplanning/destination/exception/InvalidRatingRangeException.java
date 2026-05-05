package com.randomteam2.tripplanning.destination.exception;

/**
 * Raised when {@code minRating} &gt; {@code maxRating} for destination search (HTTP 400).
 */
public class InvalidRatingRangeException extends RuntimeException {

    public InvalidRatingRangeException(String message) {
        super(message);
    }
}
