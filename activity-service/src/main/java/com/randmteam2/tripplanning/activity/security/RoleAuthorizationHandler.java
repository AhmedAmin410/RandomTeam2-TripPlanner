package com.randmteam2.tripplanning.activity.security;

public class RoleAuthorizationHandler extends AuthHandler {
    @Override
    public void handle(AuthContext context) { proceed(context); }
}
