package com.randmteam2.tripplanning.booking.security;

public abstract class AuthHandler {
    private AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public abstract void handle(AuthContext ctx) throws AuthException;

    protected void proceed(AuthContext ctx) throws AuthException {
        if (next != null) next.handle(ctx);
    }
}