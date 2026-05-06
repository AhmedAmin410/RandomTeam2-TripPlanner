package com.randmteam2.tripplanning.activity.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {
    public final HttpServletRequest request;
    public String token;
    public Claims claims;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }
}