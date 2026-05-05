package com.randmteam2.tripplanning.activity.security;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;
    public SignatureValidationHandler(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    public void handle(AuthContext context) {
        if (context.getToken() == null) return;
        if (!jwtService.isTokenValid(context.getToken())) { context.fail(401, "Invalid or expired token"); return; }
        context.setEmail(jwtService.extractEmail(context.getToken()));
        context.setUserId(jwtService.extractUserId(context.getToken()));
        context.setRole(jwtService.extractRole(context.getToken()));
        proceed(context);
    }
}
