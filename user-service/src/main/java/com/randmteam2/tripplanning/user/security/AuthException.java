package com.randmteam2.tripplanning.user.security;

public class AuthException extends Exception {
    private final int status;

    public AuthException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}