package com.example.userservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.example.userservice.entities.User;

public interface UserRepository extends JpaRepository<User, Long>{
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.cardInfos WHERE u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cardInfos"})
    Optional<User> findByEmail(String email);


    @Query("SELECT u FROM User u LEFT JOIN FETCH u.cardInfos WHERE u.id IN :ids")
    List<User> findUsersByIds(@Param("ids") List<Long> ids);

    boolean existsByEmail(String email);
}
