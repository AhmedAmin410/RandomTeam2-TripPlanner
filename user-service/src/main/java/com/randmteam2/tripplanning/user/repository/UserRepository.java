package com.randmteam2.tripplanning.user.repository;

import java.util.List;
import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByRole(Role role);

    List<User> findByNameContainingIgnoreCaseAndRole(String name, Role role);

    List<User> findByEmailContainingIgnoreCase(String email);

    List<User> findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(String name, String email);

    List<User> findByRoleAndEmailContainingIgnoreCase(Role role, String email);

    List<User> findByNameContainingIgnoreCaseAndRoleAndEmailContainingIgnoreCase(
            String name, Role role, String email
    );
}