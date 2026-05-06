package com.randomteam2.tripplanning.destination.security;

public final class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;
    private final String algorithm;

    private JwtConfigurationManager() {
        this.secret = System.getenv().getOrDefault(
                "JWT_SECRET",
                "7Wl0A/qUHLdXuZKT3nra3mBBtOot+/SF/eLo0MTq5zQ="
        );
        this.expirationMs = Long.parseLong(System.getenv().getOrDefault(
                "JWT_EXPIRATION_MS",
                "86400000"
        ));
        this.algorithm = "HMAC-SHA256";
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

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}