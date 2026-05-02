package com.randomteam2.tripplanning.destination.exception;

/**
 * Thrown when a requested domain entity does not exist (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
