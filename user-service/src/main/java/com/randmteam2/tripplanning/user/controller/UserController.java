package com.randmteam2.tripplanning.user.controller;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.SavedDestination;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.service.SavedDestinationService;
import com.randmteam2.tripplanning.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SavedDestinationService savedDestinationService;

    public UserController(UserService userService,
                          SavedDestinationService savedDestinationService) {
        this.userService = userService;
        this.savedDestinationService = savedDestinationService;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

@PutMapping("/{id}/preferences")
public User updatePreferences(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates
) {
    return userService.updatePreferences(id, updates);
}

    @GetMapping("/{id}/trip-summary")
    public ResponseEntity<UserTripSummaryDTO> getTripSummary(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserTripSummary(id));
    }


    @GetMapping("/preferences/search")
    public ResponseEntity<List<User>> searchByPreference(
            @RequestParam String key,
            @RequestParam String value) {

        return ResponseEntity.ok(userService.searchByPreference(key, value));
    }

    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String email
    ) {
        Role roleEnum = null;

        if (role != null && !role.trim().isEmpty()) {
            try {
                roleEnum = Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid role"
                );
            }
        }

        return userService.searchUsers(name, roleEnum, email);
    }

    @PostMapping("/{userId}/saved-destinations")
    public SavedDestination createSavedDestination(
            @PathVariable Long userId,
            @RequestBody SavedDestination destination
    ) {
        return userService.createSavedDestination(userId, destination);
    }


    @GetMapping("/{userId}/saved-destinations")
    public List<SavedDestination> getSavedDestinations(@PathVariable Long userId) {
        return userService.getSavedDestinations(userId);
    }


    @GetMapping("/{userId}/saved-destinations/{id}")
    public SavedDestination getSavedDestinationById(
            @PathVariable Long userId,
            @PathVariable Long id
    ) {
        return userService.getSavedDestinationById(userId, id);
    }


    @DeleteMapping("/{userId}/saved-destinations/{id}")
    public ResponseEntity<Void> deleteSavedDestination(
            @PathVariable Long userId,
            @PathVariable Long id) {

        savedDestinationService.delete(id);

        return ResponseEntity.noContent().build(); // 204
    }

    @PutMapping("/{userId}/saved-destinations/{id}")
    public SavedDestination updateSavedDestination(
            @PathVariable Long userId,
            @PathVariable Long id,
            @RequestBody SavedDestination destination) {

        return savedDestinationService.update(id, destination);
    }

    @PutMapping("/{id}/deactivate")
    public User deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }
}