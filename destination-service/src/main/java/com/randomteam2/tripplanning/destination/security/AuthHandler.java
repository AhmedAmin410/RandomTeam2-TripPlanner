package com.randomteam2.tripplanning.destination.security;

public abstract class AuthHandler {

    private AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public void handle(AuthContext context) {
        doHandle(context);

        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void doHandle(AuthContext context);
}