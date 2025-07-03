package com.example.userservice.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.services.CardInfoService;

@RestController
@RequestMapping("/api/card-infos")
public class CardInfoController {

    private final CardInfoService cardInfoService;

    public CardInfoController(CardInfoService cardInfoService) {
        this.cardInfoService = cardInfoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardInfoResponseDTO> getCardInfoById(@PathVariable Long id) {
        return ResponseEntity.ok(cardInfoService.getCardInfoById(id));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<CardInfoResponseDTO>> getCardInfosByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(cardInfoService.getCardInfosByIds(ids));
    }

    @PostMapping
    public ResponseEntity<CardInfoResponseDTO> createCardInfo(@Valid @RequestBody CardInfoRequestDTO cardInfoRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardInfoService.createCardInfo(cardInfoRequestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardInfoResponseDTO> updateCardInfo(@PathVariable Long id, @Valid @RequestBody CardInfoRequestDTO cardInfoRequestDTO) {
        return ResponseEntity.ok(cardInfoService.updateCardInfo(id, cardInfoRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCardInfo(@PathVariable Long id) {
        cardInfoService.deleteCardInfo(id);
        return ResponseEntity.noContent().build();
    }
}
