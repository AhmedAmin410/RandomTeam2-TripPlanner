package com.randmteam2.tripplanning.user.security;

import com.randmteam2.tripplanning.user.security.AuthContext;
import com.randmteam2.tripplanning.user.security.AuthException;
import com.randmteam2.tripplanning.user.security.AuthHandler;
import org.springframework.security.core.userdetails.UserDetailsService;

public class UserLoaderHandler extends AuthHandler {
    private final UserDetailsService userDetailsService;

    public UserLoaderHandler(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        try {
            userDetailsService.loadUserByUsername(ctx.claims.getSubject());
        } catch (Exception e) {
            throw new AuthException(401, "User not found");
        }
        proceed(ctx);
    }
}