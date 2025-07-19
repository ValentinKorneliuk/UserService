package com.example.userservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import com.example.userservice.entities.CardInfo;

public interface CardInfoRepository extends JpaRepository<CardInfo, UUID>{
    
    @Query(value = "SELECT * FROM card_info WHERE id IN (:ids)", nativeQuery = true)
    List<CardInfo> findCardsByIds(@Param("ids") List<UUID> ids);

    boolean existsByNumber(String number);
    
}
