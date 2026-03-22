package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import org.springframework.http.HttpStatus;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPassword(updatedUser.getPassword());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setPreferences(updatedUser.getPreferences());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
//    public List<User> searchUsers(String name, String email) {
//        return userRepository.findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
//                name == null ? "" : name,
//                email == null ? "" : email
//        );
//    }

    public User updatePreferences(Long id, Map<String, Object> updates) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> current = user.getPreferences();

        if (current == null) {
            current = new HashMap<>();
        }

        current.putAll(updates);

        user.setPreferences(current);

        return userRepository.save(user);

    public UserTripSummaryDTO getUserTripSummary(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        List<Object[]> results = userRepository.getUserTripSummary(userId);

        Object[] result = results.isEmpty() ? new Object[]{0, 0, 0, 0, 0} : results.get(0);

        Long totalTrips = ((Number) result[0]).longValue();
        Long completedTrips = ((Number) result[1]).longValue();
        Long cancelledTrips = ((Number) result[2]).longValue();
        Double totalSpent = ((Number) result[3]).doubleValue();
        Double avgBudget = ((Number) result[4]).doubleValue();

        return new UserTripSummaryDTO(
                user.getId(),
                user.getName(),
                totalTrips,
                completedTrips,
                cancelledTrips,
                totalSpent,
                avgBudget
        );
    public List<User> searchByPreference(String key, String value) {

        if (key == null || key.trim().isEmpty() ||
                value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Key and value must not be empty"
            );
        }

        return userRepository.findByPreference(key, value);
    }
}