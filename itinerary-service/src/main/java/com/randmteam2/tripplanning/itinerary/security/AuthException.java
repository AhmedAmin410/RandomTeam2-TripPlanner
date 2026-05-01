package com.randmteam2.tripplanning.itinerary.security;

public class AuthException extends Exception {
    private final int statusCode;

    public AuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}