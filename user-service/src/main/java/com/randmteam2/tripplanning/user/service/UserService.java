package com.randmteam2.tripplanning.user.service;

import com.randmteam2.tripplanning.user.model.Role;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import org.springframework.stereotype.Service;


import java.util.List;

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
                .orElseThrow(() -> new RuntimeException("User not found"));
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
}