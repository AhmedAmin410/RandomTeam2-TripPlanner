package com.randmteam2.tripplanning.itinerary.security;

public abstract class AuthHandler {
    protected AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public abstract void handle(AuthContext ctx) throws AuthException;
}