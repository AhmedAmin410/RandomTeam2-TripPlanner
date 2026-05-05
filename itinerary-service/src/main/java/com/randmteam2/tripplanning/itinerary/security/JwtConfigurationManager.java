package com.randmteam2.tripplanning.itinerary.security;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;

    private JwtConfigurationManager() {
        String envSecret = System.getenv("JWT_SECRET");
        this.secret = (envSecret != null && !envSecret.isBlank())
                ? envSecret
                : "dGhpcyBpcyBhIHNlY3VyZSByYW5kb20ga2V5IGZvciBqd3Q=";

        String envExp = System.getenv("JWT_EXPIRATION_MS");
        this.expirationMs = (envExp != null && !envExp.isBlank())
                ? Long.parseLong(envExp)
                : 86400000L;
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecret() { return secret; }
    public long getExpirationMs() { return expirationMs; }
}
