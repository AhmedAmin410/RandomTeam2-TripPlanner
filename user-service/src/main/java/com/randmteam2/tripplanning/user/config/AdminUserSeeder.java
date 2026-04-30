package com.randmteam2.tripplanning.user.config;

import com.randmteam2.tripplanning.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private final UserService userService;

    public AdminUserSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userService.seedAdminUser(
                "System Admin",
                "admin@tripplanning.com",
                "Admin@12345",
                "+201000000001"
        );
    }
}
