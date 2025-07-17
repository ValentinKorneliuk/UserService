package com.example.userservice.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;   
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

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

@ExtendWith(MockitoExtension.class)
public class CardInfoServiceTest {

    @Mock
    CardInfoRepository cardInfoRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CardInfoMapper cardInfoMapper;

    @Mock
    CacheManager cacheManager;

    @InjectMocks
    CardInfoService cardInfoService;

    @ParameterizedTest
    @ValueSource(strings = {
        "1111222233334444", 
        "1111-2222-3333-4444", 
        "1111 2222 3333 4444"
    })
    public void createCardInfo_shouldReturnCardInfoResponseDTO_whenCardInfoDoesntExist(String number){
        String normalizedNumber = number.replaceAll("[\\s-]", "");
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number(number)
            .userId(1L)
            .build();
        
        CardInfo cardInfoEntity = CardInfo.builder()
            .number(normalizedNumber)
            .build();

        User userEntity = User.builder()
            .id(1L)
            .build();

        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(1L)
            .number(normalizedNumber)
            .build();

        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoEntity.getNumber())).thenReturn(false);
        when(userRepository.findById(cardInfoRequestDTO.getUserId())).thenReturn(Optional.of(userEntity));
        when(cardInfoRepository.save(cardInfoEntity)).thenReturn(cardInfoEntity);
        when(cardInfoMapper.toDTO(cardInfoEntity)).thenReturn(cardInfoResponseDTO);

        CardInfoResponseDTO result = cardInfoService.createCardInfo(cardInfoRequestDTO);

        assertNotNull(result);
        assertEquals(cardInfoResponseDTO.getId(), result.getId());
        assertEquals(normalizedNumber, result.getNumber());
        assertEquals(cardInfoEntity.getUser(), userEntity);

        verify(cardInfoRepository).existsByNumber(cardInfoEntity.getNumber());
        verify(userRepository).findById(cardInfoRequestDTO.getUserId());
        verify(cardInfoRepository).save(cardInfoEntity);
        verify(cardInfoMapper).toDTO(cardInfoEntity);
    }

    @Test
    public void createCardInfo_shouldThrowConflictException_whenCardInfoAlreadyExists(){
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .userId(1L)
            .build();
        
        CardInfo cardInfoEntity = CardInfo.builder()
            .number("1111222233334444")
            .build();

        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            cardInfoService.createCardInfo(cardInfoRequestDTO);
        });

        assertEquals("Card info with number 1111222233334444 already exists", exception.getMessage());

        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(userRepository, never()).findById(cardInfoRequestDTO.getUserId());
        verify(cardInfoRepository, never()).save(any(CardInfo.class));
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    public void createCardInfo_shouldThrowResourceNotFoundException_whenUserDoesntExist(){
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("1111222233334444")
            .userId(1L)
            .build();
        
        CardInfo cardInfoEntity = CardInfo.builder()
            .number("1111222233334444")
            .build();

        when(cardInfoMapper.toEntity(cardInfoRequestDTO)).thenReturn(cardInfoEntity);
        when(cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())).thenReturn(false);
        when(userRepository.findById(cardInfoRequestDTO.getUserId())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.createCardInfo(cardInfoRequestDTO);
        });

        assertEquals("User not found", exception.getMessage());

        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(userRepository).findById(cardInfoRequestDTO.getUserId());
        verify(cardInfoRepository, never()).save(any(CardInfo.class));
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }
    
    @Test
    public void getCardInfoById_shouldReturnCardInfoResponseDTO_whenCardInfoExists(){
        CardInfo cardInfoEntity = CardInfo.builder()
            .id(1L)
            .build();

        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(1L)
            .number("1111222233334444")
            .build();
        
        when(cardInfoRepository.findById(1L)).thenReturn(Optional.of(cardInfoEntity));
        when(cardInfoMapper.toDTO(cardInfoEntity)).thenReturn(cardInfoResponseDTO);
        
        CardInfoResponseDTO result = cardInfoService.getCardInfoById(1L);

        assertNotNull(result);
        assertEquals(cardInfoResponseDTO.getId(), result.getId());
        assertEquals(cardInfoResponseDTO.getNumber(), result.getNumber());

        verify(cardInfoRepository).findById(1L);
        verify(cardInfoMapper).toDTO(cardInfoEntity);
    }

    @Test
    public void getCardInfoById_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist(){
        Long id = 1L;

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.getCardInfoById(id);
        });

        assertEquals("Card info not found", exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    public void getCardInfosByIds_shouldReturnCardInfoResponseDTOs_whenCardInfosExist(){
        List<Long> ids = Arrays.asList(1L, 2L);
        CardInfo cardInfoEntity1 = CardInfo.builder()
            .id(1L)
            .build();
        CardInfo cardInfoEntity2 = CardInfo.builder()
            .id(2L)
            .build();

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1, cardInfoEntity2);

        CardInfoResponseDTO cardInfoResponseDTO1 = CardInfoResponseDTO.builder()
            .id(1L)
            .build();

        CardInfoResponseDTO cardInfoResponseDTO2 = CardInfoResponseDTO.builder()
            .id(2L)
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
    public void getCardInfosByIds_shouldReturnCardInfoResponseDTOs_whenNotAllCardInfosExist(){
        List<Long> ids = Arrays.asList(1L, 2L);
        CardInfo cardInfoEntity1 = CardInfo.builder()
            .id(1L)
            .build();

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1);

        CardInfoResponseDTO cardInfoResponseDTO1 = CardInfoResponseDTO.builder()
            .id(1L)
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
    public void getCardInfosByIds_shouldReturnEmptyList_whenNoCardInfosExist(){
        List<Long> ids = Arrays.asList(1L, 2L);

        when(cardInfoRepository.findCardsByIds(ids)).thenReturn(Collections.emptyList());

        List<CardInfoResponseDTO> result = cardInfoService.getCardInfosByIds(ids);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(cardInfoRepository).findCardsByIds(ids);
        verify(cardInfoMapper, never()).toDTO(any(CardInfo.class));
    }

    @Test
    public void updateCardInfo_shouldReturnUpdatedCardInfoResponseDTO_whenCardInfoExists(){
        Long id = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .number("5555666677778888")
            .holder("NEW HOLDER")
            .expirationDate(LocalDate.of(2026, 12, 31))
            .userId(1L)
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
            .build();

        CardInfoResponseDTO cardInfoResponseDTO = CardInfoResponseDTO.builder()
            .id(id)
            .number(cardInfoRequestDTO.getNumber())
            .holder(cardInfoRequestDTO.getHolder())
            .expirationDate(cardInfoRequestDTO.getExpirationDate())
            .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
        when(userRepository.existsById(cardInfoRequestDTO.getUserId())).thenReturn(true);
        doNothing().when(cardInfoMapper).updateEntity(cardInfoRequestDTO, cardInfoEntity);
        when(cardInfoRepository.save(cardInfoEntity)).thenReturn(updatedCardInfoEntity);
        when(cardInfoMapper.toDTO(updatedCardInfoEntity)).thenReturn(cardInfoResponseDTO);

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
    }

    @Test
    public void updateCardInfo_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist(){
        Long id = 1L;
        CardInfoRequestDTO cardInfoRequestDTO = CardInfoRequestDTO.builder()
            .build();

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.updateCardInfo(id, cardInfoRequestDTO);
        });

        assertEquals("Card info not found", exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository, never()).existsByNumber(any());
        verify(cardInfoMapper, never()).updateEntity(any(), any());
        verify(cardInfoRepository, never()).save(any());
        verify(cardInfoMapper, never()).toDTO(any());
    }

    @Test
    public void updateCardInfo_shouldThrowConflictException_whenCardInfoNumberAlreadyExists(){
        Long id = 1L;
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

        assertEquals("Card info with number 5555666677778888 already exists", exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository).existsByNumber(cardInfoRequestDTO.getNumber());
        verify(cardInfoMapper, never()).updateEntity(any(), any());
        verify(cardInfoRepository, never()).save(any());
        verify(cardInfoMapper, never()).toDTO(any());
    }

    @Test
    public void deleteCardInfo_shouldDeleteCardInfo_whenCardInfoExists(){
        Long id = 1L;
        Long userId = 1L;

        User userEntity = User.builder()
            .id(userId)
            .build();

        CardInfo cardInfoEntity = CardInfo.builder()
            .id(id)
            .user(userEntity)
            .build();

        Cache cache = mock(Cache.class);

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
        doNothing().when(cardInfoRepository).delete(cardInfoEntity);
        when(cacheManager.getCache("users")).thenReturn(cache);
        doNothing().when(cache).evict(userId);

        cardInfoService.deleteCardInfo(id);

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository).delete(cardInfoEntity);
        verify(cacheManager).getCache("users");
        verify(cache).evict(userId);
    }

    @Test
    public void deleteCardInfo_shouldThrowResourceNotFoundException_whenCardInfoDoesntExist(){
        Long id = 1L;

        when(cardInfoRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cardInfoService.deleteCardInfo(id);
        });

        assertEquals("Card info not found", exception.getMessage());

        verify(cardInfoRepository).findById(id);
        verify(cardInfoRepository, never()).delete(any());
        verify(cacheManager, never()).getCache("users");
    }
}
