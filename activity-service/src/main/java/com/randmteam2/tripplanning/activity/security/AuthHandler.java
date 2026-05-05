package com.randmteam2.tripplanning.activity.security;

public abstract class AuthHandler {
    protected AuthHandler next;
    public AuthHandler setNext(AuthHandler next) { this.next = next; return next; }
    public abstract void handle(AuthContext context);
    protected void proceed(AuthContext context) { if (next != null && !context.isFailed()) next.handle(context); }
}
