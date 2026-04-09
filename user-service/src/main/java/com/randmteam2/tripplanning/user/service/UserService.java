package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.dto.UserTripSummaryDTO;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.Status;
import com.randmteam2.tripplanning.user.model.SavedDestination;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final SavedDestinationService savedDestinationService;


    public UserService(UserRepository userRepository,
                       SavedDestinationService savedDestinationService) {
        this.userRepository = userRepository;
        this.savedDestinationService = savedDestinationService;
    }

    public User createUser(User user) {

        if (user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        if (user.getStatus() == null) {
            user.setStatus(Status.ACTIVE);
        }

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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updatedUser.getName() != null)
            user.setName(updatedUser.getName());

        if (updatedUser.getEmail() != null)
            user.setEmail(updatedUser.getEmail());

        if (updatedUser.getPhone() != null)
            user.setPhone(updatedUser.getPhone());

        if (updatedUser.getRole() != null)
            user.setRole(updatedUser.getRole());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
    public List<User> searchUsers(String name, Role role, String email) {

        if (name != null && role != null && email != null) {
            return userRepository
                    .findByNameContainingIgnoreCaseAndRoleAndEmailContainingIgnoreCase(name, role, email);
        }

        if (name != null && role != null) {
            return userRepository
                    .findByNameContainingIgnoreCaseAndRole(name, role);
        }

        if (name != null && email != null) {
            return userRepository
                    .findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(name, email);
        }

        if (role != null && email != null) {
            return userRepository
                    .findByRoleAndEmailContainingIgnoreCase(role, email);
        }

        if (name != null) {
            return userRepository.findByNameContainingIgnoreCase(name);
        }

        if (role != null) {
            return userRepository.findByRole(role);
        }

        if (email != null) {
            return userRepository.findByEmailContainingIgnoreCase(email);
        }

        return userRepository.findAll();
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
                    HttpStatus.BAD_REQUEST,
                    "Key and value must not be empty"
            );
        }

        return userRepository.findByPreference(key, value);
    }
    public SavedDestination createSavedDestination(Long userId, SavedDestination destination) {

        getUserById(userId); // ensure user exists

        User user = getUserById(userId);
        destination.setUser(user);

        return savedDestinationService.create(destination);
    }

    public List<SavedDestination> getSavedDestinations(Long userId) {

        getUserById(userId);

        return savedDestinationService.getByUserId(userId);
    }
    public SavedDestination getSavedDestinationById(Long userId, Long id) {

        getUserById(userId);

        SavedDestination dest = savedDestinationService.getById(id);

        if (!dest.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return dest;
    }
    public void deleteSavedDestination(Long userId, Long id) {

        getUserById(userId);

        SavedDestination dest = savedDestinationService.getById(id);

        if (!dest.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        savedDestinationService.delete(id);
    }



}