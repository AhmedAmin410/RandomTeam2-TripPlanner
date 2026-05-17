package com.randmteam2.tripplanning.user.controller;

import com.randmteam2.tripplanning.user.dto.LoginRequest;
import com.randmteam2.tripplanning.user.events.UserRabbitEventPublisher;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.observer.UserEventPublisher;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import com.randmteam2.tripplanning.user.security.JwtConfigurationManager;
import com.randmteam2.tripplanning.user.security.JwtService;
import com.randmteam2.tripplanning.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserEventPublisher eventPublisher;
    private final UserRabbitEventPublisher rabbitEventPublisher;

    public AuthController(UserService userService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          UserEventPublisher eventPublisher,
                          UserRabbitEventPublisher rabbitEventPublisher) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.rabbitEventPublisher = rabbitEventPublisher;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        if (isBlank(user.getName()) || isBlank(user.getEmail())
                || isBlank(user.getPassword()) || isBlank(user.getPhone())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name, email, password, and phone are required"));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        if (userRepository.existsByPhone(user.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Phone already registered"));
        }

        User created = userService.createUser(user);

        // Existing local observer event
        eventPublisher.notifyObservers("REGISTERED",
                Map.of("userId", created.getId(), "email", created.getEmail()));

        // Requirement 2 / S1-EVENTS:
        // Publish user.registered event to RabbitMQ user.events exchange
        rabbitEventPublisher.publishUserRegistered(created);

        String token = jwtService.generateToken(created);
        long expiresIn = JwtConfigurationManager.getInstance().getExpirationMs();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", token, "expiresIn", expiresIn));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        eventPublisher.notifyObservers("LOGGED_IN", Map.of("userId", user.getId()));

        String token = jwtService.generateToken(user);
        long expiresIn = JwtConfigurationManager.getInstance().getExpirationMs();

        return ResponseEntity.ok(Map.of("token", token, "expiresIn", expiresIn));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}