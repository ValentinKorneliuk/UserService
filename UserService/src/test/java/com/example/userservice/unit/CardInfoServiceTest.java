package com.example.userservice.unit;

import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.cache.Cache;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;

import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.mappers.CardInfoMapper;
import com.example.userservice.services.CardInfoService;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;

import static com.example.userservice.exceptions.ConflictException.CARD_INFO_ALREADY_EXISTS;
import static com.example.userservice.exceptions.ResourceNotFoundException.CARD_INFO_NOT_FOUND;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CardInfoServiceTest {

    @Mock
    CardInfoRepository cardInfoRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CardInfoMapper cardInfoMapper;

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache cache;

    @Mock
    UserDetailsProvider userDetailsProvider;

    @InjectMocks
    CardInfoService cardInfoService;

    @ParameterizedTest
    @ValueSource(strings = {
            "1111222233334444",
            "1111-2222-3333-4444",
            "1111 2222 3333 4444"
    })
    void createCardInfo_shouldReturnCardInfoResponseDTO_whenCardInfoDoesntExist(String number) {
        String normalizedNumber = number.replaceAll("[\\s-]", "");
        UUID userId = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .number(number)
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .number(normalizedNumber)
                .build();

        User userEntity = User.builder()
                .id(userId)
                .build();

        UUID cardInfoId = UUID.randomUUID();
        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
                .id(cardInfoId)
                .number(normalizedNumber)
                .build();

        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoEntity.getNumber())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userDetailsProvider.getUserId()).thenReturn(userId);
        when(cardInfoRepository.save(cardInfoEntity)).thenReturn(cardInfoEntity);
        when(cardInfoMapper.toDTO(cardInfoEntity)).thenReturn(cardInfoResponseDTO);
        when(cacheManager.getCache("users")).thenReturn(cache);
        doNothing().when(cache).evict(any());

        CardInfoResponseDTO result = cardInfoService.createCardInfo(cardInfoRequestDTO);

        assertNotNull(result);
        assertEquals(cardInfoResponseDTO.getId(), result.getId());
        assertEquals(normalizedNumber, result.getNumber());
        assertEquals(cardInfoEntity.getUser(), userEntity);

        verify(cardInfoRepository).existsByNumber(cardInfoEntity.getNumber());
        verify(userRepository).findById(userId);
        verify(cardInfoRepository).save(cardInfoEntity);
        verify(cardInfoMapper).toDTO(cardInfoEntity);
        verify(cacheManager).getCache("users");
        verify(cache).evict(any());
    }

    @Test
    void createCardInfo_shouldThrowConflictException_whenCardInfoAlreadyExists() {
        UUID userId = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .number("1111222233334444")
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .number("1111222233334444")
                .build();

        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            cardInfoService.createCardInfo(cardInfoRequestDTO);
        });

        assertEquals(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber()), exception.getMessage());

        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(userRepository, never()).findById(userId);
        verify(cardInfoRepository, never()).save(any(CardInfo.class));
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    void createCardInfo_shouldThrowResourceNotFoundException_whenUserDoesntExist() {
        UUID userId = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .number("1111222233334444")
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .number("1111222233334444")
                .build();

        when(userDetailsProvider.getUserId()).thenReturn(userId);
        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.createCardInfo(cardInfoRequestDTO);
        });

        assertEquals("User not found", exception.getMessage());

        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(userRepository).findById(userId);
        verify(cardInfoRepository, never()).save(any(CardInfo.class));
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    void getCardInfoById_shouldReturnCardInfoResponseDTO_whenCardInfoExists() {
        UUID cardInfoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CardInfo cardInfoEntity = CardInfo.builder()
                .id(cardInfoId)
                .user(User.builder()
                        .id(userId)
                        .build())
                .build();

        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
                .id(cardInfoId)
                .number("1111222233334444")
                .build();

        when(userDetailsProvider.getUserId()).thenReturn(userId);
        when(cardInfoRepository.findById(cardInfoId)).thenReturn(Optional.of(cardInfoEntity));
        when(cardInfoMapper.toDTO(cardInfoEntity)).thenReturn(cardInfoResponseDTO);

        CardInfoResponseDTO result = cardInfoService.getCardInfoById(cardInfoId);

        assertNotNull(result);
        assertEquals(cardInfoResponseDTO.getId(), result.getId());
        assertEquals(cardInfoResponseDTO.getNumber(), result.getNumber());

        verify(cardInfoRepository).findById(cardInfoId);
        verify(cardInfoMapper).toDTO(cardInfoEntity);
    }

    @Test
    void getCardInfoById_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist() {
        UUID id = UUID.randomUUID();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.getCardInfoById(id);
        });

        assertEquals(CARD_INFO_NOT_FOUND, exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    void getCardInfosByIds_shouldReturnCardInfoResponseDTOs_whenCardInfosExist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(id1, id2);
        CardInfo cardInfoEntity1 = CardInfo.builder()
                .id(id1)
                .build();
        CardInfo cardInfoEntity2 = CardInfo.builder()
                .id(id2)
                .build();

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1, cardInfoEntity2);

        CardInfoResponseDTO cardInfoResponseDTO1 = CardInfoResponseDTO.builder()
                .id(id1)
                .build();

        CardInfoResponseDTO cardInfoResponseDTO2 = CardInfoResponseDTO.builder()
                .id(id2)
                .build();

        when(cardInfoRepository.findCardsByIds(ids)).thenReturn(cardInfoEntities);
        when(cardInfoMapper.toDTO(cardInfoEntity1)).thenReturn(cardInfoResponseDTO1);
        when(cardInfoMapper.toDTO(cardInfoEntity2)).thenReturn(cardInfoResponseDTO2);

        List<CardInfoResponseDTO> result = cardInfoService.getCardInfosByIds(ids);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cardInfoResponseDTO1, result.get(0));
        assertEquals(cardInfoResponseDTO2, result.get(1));

        verify(cardInfoRepository).findCardsByIds(ids);
        verify(cardInfoMapper).toDTO(cardInfoEntity1);
        verify(cardInfoMapper).toDTO(cardInfoEntity2);
    }

    @Test
    void getCardInfosByIds_shouldReturnCardInfoResponseDTOs_whenNotAllCardInfosExist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(id1, id2);
        CardInfo cardInfoEntity1 = CardInfo.builder()
                .id(id1)
                .build();

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1);

        CardInfoResponseDTO cardInfoResponseDTO1 = CardInfoResponseDTO.builder()
                .id(id1)
                .build();

        when(cardInfoRepository.findCardsByIds(ids)).thenReturn(cardInfoEntities);
        when(cardInfoMapper.toDTO(cardInfoEntity1)).thenReturn(cardInfoResponseDTO1);

        List<CardInfoResponseDTO> result = cardInfoService.getCardInfosByIds(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(cardInfoResponseDTO1, result.get(0));

        verify(cardInfoRepository).findCardsByIds(ids);
        verify(cardInfoMapper).toDTO(cardInfoEntity1);
    }

    @Test
    void getCardInfosByIds_shouldReturnEmptyList_whenNoCardInfosExist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = Arrays.asList(id1, id2);

        when(cardInfoRepository.findCardsByIds(ids)).thenReturn(Collections.emptyList());

        List<CardInfoResponseDTO> result = cardInfoService.getCardInfosByIds(ids);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(cardInfoRepository).findCardsByIds(ids);
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    void updateCardInfo_shouldReturnUpdatedCardInfoResponseDTO_whenCardInfoExists() {
        UUID id = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .number("5555666677778888")
                .holder("NEW HOLDER")
                .expirationDate(LocalDate.of(2026, 12, 31))
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .id(id)
                .number("1111222233334444")
                .holder("OLD HOLDER")
                .expirationDate(LocalDate.of(2025, 1, 1))
                .build();

        CardInfo updatedCardInfoEntity = CardInfo.builder()
                .id(id)
                .number(cardInfoRequestDTO.getNumber())
                .holder(cardInfoRequestDTO.getHolder())
                .expirationDate(cardInfoRequestDTO.getExpirationDate())
                .user(User.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .build())
                .build();

        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
                .id(id)
                .number(cardInfoRequestDTO.getNumber())
                .holder(cardInfoRequestDTO.getHolder())
                .expirationDate(cardInfoRequestDTO.getExpirationDate())
                .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
        doNothing().when(cardInfoMapper).updateEntity(cardInfoRequestDTO, cardInfoEntity);
        when(cardInfoRepository.save(cardInfoEntity)).thenReturn(updatedCardInfoEntity);
        when(cardInfoMapper.toDTO(updatedCardInfoEntity)).thenReturn(cardInfoResponseDTO);
        when(cacheManager.getCache("users")).thenReturn(cache);
        doNothing().when(cache).evict(any());

        CardInfoResponseDTO result = cardInfoService.updateCardInfo(id, cardInfoRequestDTO);

        assertNotNull(result);
        assertEquals(cardInfoResponseDTO.getId(), result.getId());
        assertEquals(cardInfoResponseDTO.getNumber(), result.getNumber());
        assertEquals(cardInfoResponseDTO.getHolder(), result.getHolder());
        assertEquals(cardInfoResponseDTO.getExpirationDate(), result.getExpirationDate());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoMapper).updateEntity(cardInfoRequestDTO, cardInfoEntity);
        verify(cardInfoRepository).save(cardInfoEntity);
        verify(cardInfoMapper).toDTO(updatedCardInfoEntity);
        verify(cacheManager).getCache("users");
        verify(cache).evict(any());
    }

    @Test
    void updateCardInfo_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist() {
        UUID id = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.updateCardInfo(id, cardInfoRequestDTO);
        });

        assertEquals(CARD_INFO_NOT_FOUND, exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository, never()).existsByNumber(any());
        verify(cardInfoMapper, never()).updateEntity(any(), any());
        verify(cardInfoRepository, never()).save(any());
        verify(cardInfoMapper, never()).toDTO(any());
    }

    @Test
    void updateCardInfo_shouldThrowConflictException_whenCardInfoNumberAlreadyExists() {
        UUID id = UUID.randomUUID();
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
                .number("5555666677778888")
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .id(id)
                .number("1111222233334444")
                .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
        when(cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            cardInfoService.updateCardInfo(id, cardInfoRequestDTO);
        });

        assertEquals(String.format(CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber()), exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(cardInfoMapper, never()).updateEntity(any(), any());
        verify(cardInfoRepository, never()).save(any());
        verify(cardInfoMapper, never()).toDTO(any());
    }

    @Test
    void deleteCardInfo_shouldDeleteCardInfo_whenCardInfoExists() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User userEntity = User.builder()
                .id(userId)
                .build();

        CardInfo cardInfoEntity = CardInfo.builder()
                .id(id)
                .user(userEntity)
                .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
        doNothing().when(cardInfoRepository).delete(cardInfoEntity);
        when(cacheManager.getCache("users")).thenReturn(cache);
        doNothing().when(cache).evict(userId);
        when(userDetailsProvider.getUserId()).thenReturn(userId);

        cardInfoService.deleteCardInfo(id);

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository).delete(cardInfoEntity);
        verify(cacheManager).getCache("users");
        verify(cache).evict(userId);
    }

    @Test
    void deleteCardInfo_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist() {
        UUID id = UUID.randomUUID();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.deleteCardInfo(id);
        });

        assertEquals(CARD_INFO_NOT_FOUND, exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository, never()).delete(any());
        verify(cacheManager, never()).getCache("users");
    }
}