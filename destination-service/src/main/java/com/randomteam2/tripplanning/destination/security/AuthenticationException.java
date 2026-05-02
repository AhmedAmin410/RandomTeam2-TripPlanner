package com.randomteam2.tripplanning.destination.security;

public class AuthenticationException extends RuntimeException {

    private final int status;

    public AuthenticationException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}