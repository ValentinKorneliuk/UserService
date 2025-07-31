package com.example.userservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.userservice.entities.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.cardInfos WHERE u.id = :id")
    Optional<User> findById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"cardInfos"})
    Optional<User> findByEmail(String email);


    @Query("SELECT u FROM User u LEFT JOIN FETCH u.cardInfos WHERE u.id IN :ids")
    List<User> findUsersByIds(@Param("ids") List<UUID> ids);

    boolean existsByEmail(String email);
}