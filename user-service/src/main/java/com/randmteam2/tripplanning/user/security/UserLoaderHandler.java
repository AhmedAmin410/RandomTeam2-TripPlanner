package com.randmteam2.tripplanning.user.security;

public class UserLoaderHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (ctx.claims == null || ctx.claims.getSubject() == null) {
            throw new AuthException(401, "Token missing subject claim");
        }
        proceed(ctx);
    }
}
