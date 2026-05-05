package com.randmteam2.tripplanning.activity.security;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext context) {
        String header = context.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return;
        context.setToken(header.substring(7));
        proceed(context);
    }
}
