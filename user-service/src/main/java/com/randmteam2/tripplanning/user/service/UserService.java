package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.SavedDestination;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.UserStatus;
import com.randmteam2.tripplanning.user.repository.SavedDestinationRepository;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SavedDestinationRepository savedDestinationRepository;

    public UserService(UserRepository userRepository,
                       SavedDestinationRepository savedDestinationRepository) {
        this.userRepository = userRepository;
        this.savedDestinationRepository = savedDestinationRepository;
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

        if (updatedUser.getStatus() != null &&
                updatedUser.getStatus() == UserStatus.DEACTIVATED &&
                updatedUser.getName() == null &&
                updatedUser.getEmail() == null &&
                updatedUser.getPassword() == null &&
                updatedUser.getPhone() == null &&
                updatedUser.getRole() == null &&
                updatedUser.getPreferences() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User has active itinerary"
            );
        }

        if (updatedUser.getName() != null)
            existingUser.setName(updatedUser.getName());

        if (updatedUser.getEmail() != null)
            existingUser.setEmail(updatedUser.getEmail());

        if (updatedUser.getPassword() != null)
            existingUser.setPassword(updatedUser.getPassword());

        if (updatedUser.getPhone() != null)
            existingUser.setPhone(updatedUser.getPhone());

        if (updatedUser.getRole() != null)
            existingUser.setRole(updatedUser.getRole());

        if (updatedUser.getPreferences() != null)
            existingUser.setPreferences(updatedUser.getPreferences());

        if (updatedUser.getStatus() != null)
            existingUser.setStatus(updatedUser.getStatus());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

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
    }

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
    }

    public List<User> searchByPreference(String key, String value) {
        if (key == null || key.trim().isEmpty() ||
                value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Key and value must not be empty");
        }
        return userRepository.findByPreference(key, value);
    }

    public List<User> searchUsers(String name, Role role, String email) {
        return userRepository.searchUsers(
                name,
                role != null ? role.name() : null,
                email
        );
    }

    public SavedDestination createSavedDestination(Long userId, SavedDestination destination) {
        User user = getUserById(userId);
        destination.setUser(user);
        return savedDestinationRepository.save(destination);
    }

    public List<SavedDestination> getSavedDestinations(Long userId) {
        getUserById(userId);
        return savedDestinationRepository.findByUser_Id(userId);
    }

    public SavedDestination getSavedDestinationById(Long userId, Long id) {
        getUserById(userId);
        return savedDestinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SavedDestination not found"));
    }

    public User deactivateUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User has active itineraries"
            );
        }

        user.setStatus(UserStatus.DEACTIVATED);
        return userRepository.save(user);
    }
}