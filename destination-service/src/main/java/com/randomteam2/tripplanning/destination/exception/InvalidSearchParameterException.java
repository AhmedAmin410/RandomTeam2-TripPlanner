package com.randomteam2.tripplanning.destination.exception;

/**
 * Raised for invalid search inputs such as an unknown category (HTTP 400).
 */
public class InvalidSearchParameterException extends RuntimeException {

    public InvalidSearchParameterException(String message) {
        super(message);
    }
}
