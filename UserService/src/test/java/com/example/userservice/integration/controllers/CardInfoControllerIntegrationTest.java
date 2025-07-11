package com.example.userservice.integration.controllers;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;  
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static com.example.userservice.exceptions.ConflictException.CARD_INFO_ALREADY_EXISTS;
import static com.example.userservice.exceptions.ResourceNotFoundException.CARD_INFO_NOT_FOUND;
import static com.example.userservice.exceptions.ResourceNotFoundException.USER_NOT_FOUND;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import com.example.userservice.controllers.CardInfoController;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;
import com.example.userservice.services.CardInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CardInfoController.class)
class CardInfoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardInfoService cardInfoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCardInfoById_shouldReturnCardInfo() throws Exception {
        Long cardInfoId = 1L;
        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(cardInfoId)
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build();
            
        when(cardInfoService.getCardInfoById(cardInfoId)).thenReturn(cardInfoResponseDTO);
        
        mockMvc.perform(get("/api/card-infos/{id}", cardInfoId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cardInfoId))
            .andExpect(jsonPath("$.number").value("1111222233334444"))
            .andExpect(jsonPath("$.holder").value("John Doe"))
            .andExpect(jsonPath("$.expirationDate").value("2027-01-01"));
    }  

    @Test
    void getCardInfoById_shouldReturnNotFound() throws Exception {
        Long cardInfoId = 1L;

        when(cardInfoService.getCardInfoById(cardInfoId)).thenThrow(new ResourceNotFoundException(CARD_INFO_NOT_FOUND));
        
        mockMvc.perform(get("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Card info not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

    @Test
    void getCardInfosByIds_shouldReturnCardInfos() throws Exception {
        List<Long> cardInfoIds = Arrays.asList(1L, 2L, 3L);
        List<CardInfoResponseDTO> cardInfoResponseDTOs = new ArrayList<>();
        cardInfoResponseDTOs.add(CardInfoResponseDTO.builder()
            .id(1L)
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build());
        cardInfoResponseDTOs.add(CardInfoResponseDTO.builder()
            .id(2L)
            .number("2222333344445555")
            .holder("Jane Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build());
        cardInfoResponseDTOs.add(CardInfoResponseDTO.builder()
            .id(3L)
            .number("3333444455556666")
            .holder("Jim Beam")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build());

        when(cardInfoService.getCardInfosByIds(cardInfoIds)).thenReturn(cardInfoResponseDTOs);

        mockMvc.perform(get("/api/card-infos/ids")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .param("ids", cardInfoIds.stream().map(String::valueOf).collect(Collectors.joining(","))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size()").value(cardInfoResponseDTOs.size()))
            .andExpect(jsonPath("$[0].id").value(cardInfoResponseDTOs.get(0).getId()))
            .andExpect(jsonPath("$[0].number").value(cardInfoResponseDTOs.get(0).getNumber()))
            .andExpect(jsonPath("$[0].holder").value(cardInfoResponseDTOs.get(0).getHolder()))
            .andExpect(jsonPath("$[0].expirationDate").value(cardInfoResponseDTOs.get(0).getExpirationDate().toString()));
    }

    @Test
    void getCardInfosByIds_shouldReturnEmptyList() throws Exception {
        List<Long> cardInfoIds = Arrays.asList();
        when(cardInfoService.getCardInfosByIds(cardInfoIds)).thenReturn(new ArrayList<>());
        
        mockMvc.perform(get("/api/card-infos/ids")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .param("ids", cardInfoIds.stream().map(String::valueOf).collect(Collectors.joining(","))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void getCardInfosByIds_shouldReturnBadRequestOnAbsentIds() throws Exception {
        List<Long> cardInfoIds = Arrays.asList();
        when(cardInfoService.getCardInfosByIds(cardInfoIds)).thenThrow(new IllegalArgumentException("ids parameter is missing"));
        
        mockMvc.perform(get("/api/card-infos/ids")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("ids parameter is missing"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Missing request parameter"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/card-infos/ids"));
    }

    @Test
    void createCardInfo_shouldReturnCardInfo() throws Exception {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();
        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(1L)
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build();

        when(cardInfoService.createCardInfo(any(CardInfoRequestDTO.class))).thenReturn(cardInfoResponseDTO);
        
        mockMvc.perform(post("/api/card-infos")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(cardInfoResponseDTO.getId()))
            .andExpect(jsonPath("$.number").value(cardInfoResponseDTO.getNumber()))
            .andExpect(jsonPath("$.holder").value(cardInfoResponseDTO.getHolder()))
            .andExpect(jsonPath("$.expirationDate").value(cardInfoResponseDTO.getExpirationDate().toString()))
            .andExpect(jsonPath("$.userId").value(cardInfoResponseDTO.getUserId()));
    }

    @Test
    void createCardInfo_shouldReturnBadRequest_whenBlankFieldsAndNullExpirationDate() throws Exception {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("")
            .holder(" ")
            .expirationDate(null)
            .userId(null)
            .build();

        mockMvc.perform(post("/api/card-infos")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("number: Card number is required")))
            .andExpect(jsonPath("$.messages", hasItem("number: Card number must be 16 digits, optionally separated by spaces or hyphens")))
            .andExpect(jsonPath("$.messages", hasItem("holder: Card holder is required")))
            .andExpect(jsonPath("$.messages", hasItem("expirationDate: Expiration date is required")))
            .andExpect(jsonPath("$.messages", hasItem("userId: User ID is required")))
            .andExpect(jsonPath("$.path").value("/api/card-infos"));
    }

    @Test
    void createCardInfo_shouldReturnBadRequest_whenExpirationDateIsInThePast() throws Exception {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2024, 1, 1))
            .userId(1L)
            .build();

        mockMvc.perform(post("/api/card-infos")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("expirationDate: Expiration date must be in the future")))
            .andExpect(jsonPath("$.path").value("/api/card-infos"));
    }

    @Test
    void createCardInfo_shouldReturnConflict_whenCardNumberAlreadyExists() throws Exception {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();

        when(cardInfoService.createCardInfo(any(CardInfoRequestDTO.class)))
            .thenThrow(new ConflictException(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())));

        mockMvc.perform(post("/api/card-infos")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())))
            .andExpect(jsonPath("$.path").value("/api/card-infos"));
    }

    @Test
    void createCardInfo_shouldReturnResourceNotFoundException_whenUserNotFound() throws Exception {
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();

        when(cardInfoService.createCardInfo(any(CardInfoRequestDTO.class)))
            .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(post("/api/card-infos")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.message").value(USER_NOT_FOUND))
            .andExpect(jsonPath("$.path").value("/api/card-infos"));
    }

    @Test
    void updateCardInfo_shouldReturnCardInfo() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();
        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(cardInfoId)
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .build();

        when(cardInfoService.updateCardInfo(eq(cardInfoId), any(CardInfoRequestDTO.class))).thenReturn(cardInfoResponseDTO);
        
        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cardInfoResponseDTO.getId()))
            .andExpect(jsonPath("$.number").value(cardInfoResponseDTO.getNumber()))
            .andExpect(jsonPath("$.holder").value(cardInfoResponseDTO.getHolder()))
            .andExpect(jsonPath("$.expirationDate").value(cardInfoResponseDTO.getExpirationDate().toString()));
    }

    @Test
    void updateCardInfo_shouldReturnBadRequest_whenBlankFieldsAndNullExpirationDate() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("")
            .holder(" ")
            .expirationDate(null)
            .userId(null)
            .build();

        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("number: Card number is required")))
            .andExpect(jsonPath("$.messages", hasItem("number: Card number must be 16 digits, optionally separated by spaces or hyphens")))
            .andExpect(jsonPath("$.messages", hasItem("holder: Card holder is required")))
            .andExpect(jsonPath("$.messages", hasItem("expirationDate: Expiration date is required")))
            .andExpect(jsonPath("$.messages", hasItem("userId: User ID is required")))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

    @Test
    void updateCardInfo_shouldReturnBadRequest_whenExpirationDateIsInThePast() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2024, 1, 1))
            .userId(1L)
            .build();

        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.messages", hasItem("expirationDate: Expiration date must be in the future")))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

    @Test
    void updateCardInfo_shouldReturnConflict_whenCardNumberAlreadyExists() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();

        when(cardInfoService.updateCardInfo(eq(cardInfoId), any(CardInfoRequestDTO.class)))
            .thenThrow(new ConflictException(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())));

        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }  
    
    @Test
    void updateCardInfo_shouldReturnResourceNotFoundException_whenCardInfoNotFound() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();

        when(cardInfoService.updateCardInfo(eq(cardInfoId), any(CardInfoRequestDTO.class)))
        .thenThrow(new ResourceNotFoundException(CARD_INFO_NOT_FOUND));

        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.message").value("Card info not found"))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

    @Test
    void updateCardInfo_shouldReturnResourceNotFoundException_whenUserNotFound() throws Exception {
        Long cardInfoId = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .holder("John Doe")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .userId(1L)
            .build();

        when(cardInfoService.updateCardInfo(eq(cardInfoId), any(CardInfoRequestDTO.class)))
        .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(put("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardInfoRequestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

    @Test
    void deleteCardInfo_shouldReturnNoContent() throws Exception {
        Long cardInfoId = 1L;
        doNothing().when(cardInfoService).deleteCardInfo(cardInfoId);
        
        mockMvc.perform(delete("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteCardInfo_shouldReturnResourceNotFoundException_whenCardInfoNotFound() throws Exception {
        Long cardInfoId = 1L;
        doThrow(new ResourceNotFoundException(CARD_INFO_NOT_FOUND))
            .when(cardInfoService).deleteCardInfo(cardInfoId);

        mockMvc.perform(delete("/api/card-infos/{id}", cardInfoId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not found"))
            .andExpect(jsonPath("$.message").value("Card info not found"))
            .andExpect(jsonPath("$.path").value("/api/card-infos/" + cardInfoId));
    }

}
