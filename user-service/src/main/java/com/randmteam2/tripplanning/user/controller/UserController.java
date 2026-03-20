package com.randmteam2.tripplanning.user.controller;

import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
//    @GetMapping("/search")
//    public List<User> searchUsers(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String email
//    ) {
//        return userService.searchUsers(name, email);
//    }

@PutMapping("/{id}/preferences")
public User updatePreferences(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates
) {
    return userService.updatePreferences(id, updates);
}

}