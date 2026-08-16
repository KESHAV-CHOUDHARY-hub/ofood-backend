package com.ofood.auth.repository;

import com.ofood.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmail(String email);
}
