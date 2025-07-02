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
import org.springframework.cache.support.SimpleCacheManager;

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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber(number);
        cardInfoRequestDTO.setUserId(1L);
        
        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setNumber(normalizedNumber);

        User userEntity = new User();
        userEntity.setId(1L);

        CardInfoResponseDTO cardInfoResponseDTO = new CardInfoResponseDTO();
        cardInfoResponseDTO.setId(1L);
        cardInfoResponseDTO.setNumber(normalizedNumber);

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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber("1111222233334444");
        cardInfoRequestDTO.setUserId(1L);
        
        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setNumber("1111222233334444");

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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber("1111222233334444");
        cardInfoRequestDTO.setUserId(1L);
        
        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setNumber("1111222233334444");

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
        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setId(1L);

        CardInfoResponseDTO cardInfoResponseDTO = new CardInfoResponseDTO();
        cardInfoResponseDTO.setId(1L);
        cardInfoResponseDTO.setNumber("1111222233334444");
        
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
        CardInfo cardInfoEntity1 = new CardInfo();
        cardInfoEntity1.setId(1L);
        CardInfo cardInfoEntity2 = new CardInfo();
        cardInfoEntity2.setId(2L);

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1, cardInfoEntity2);

        CardInfoResponseDTO cardInfoResponseDTO1 = new CardInfoResponseDTO();
        cardInfoResponseDTO1.setId(1L);

        CardInfoResponseDTO cardInfoResponseDTO2 = new CardInfoResponseDTO();
        cardInfoResponseDTO2.setId(2L);

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
        CardInfo cardInfoEntity1 = new CardInfo();
        cardInfoEntity1.setId(1L);

        List<CardInfo> cardInfoEntities = Arrays.asList(cardInfoEntity1);

        CardInfoResponseDTO cardInfoResponseDTO1 = new CardInfoResponseDTO();
        cardInfoResponseDTO1.setId(1L);

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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber("5555666677778888");
        cardInfoRequestDTO.setHolder("NEW HOLDER");
        cardInfoRequestDTO.setExpirationDate(LocalDate.of(2026, 12, 31));
        cardInfoRequestDTO.setUserId(1L);
        
        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setId(id);
        cardInfoEntity.setNumber("1111222233334444");
        cardInfoEntity.setHolder("OLD HOLDER");
        cardInfoEntity.setExpirationDate(LocalDate.of(2025, 1, 1));

        CardInfo updatedCardInfoEntity = new CardInfo();
        updatedCardInfoEntity.setId(id);
        updatedCardInfoEntity.setNumber(cardInfoRequestDTO.getNumber());
        updatedCardInfoEntity.setHolder(cardInfoRequestDTO.getHolder());
        updatedCardInfoEntity.setExpirationDate(cardInfoRequestDTO.getExpirationDate());

        CardInfoResponseDTO cardInfoResponseDTO = new CardInfoResponseDTO();
        cardInfoResponseDTO.setId(id);
        cardInfoResponseDTO.setNumber(cardInfoRequestDTO.getNumber());
        cardInfoResponseDTO.setHolder(cardInfoRequestDTO.getHolder());
        cardInfoResponseDTO.setExpirationDate(cardInfoRequestDTO.getExpirationDate());

        when(cardInfoRepository.findById(id)).thenReturn(Optional.of(cardInfoEntity));
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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();

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
        CardInfoRequestDTO cardInfoRequestDTO = new CardInfoRequestDTO();
        cardInfoRequestDTO.setNumber("5555666677778888");

        CardInfo cardInfoEntity = new CardInfo();
        cardInfoEntity.setId(id);
        cardInfoEntity.setNumber("1111222233334444");

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

        CardInfo cardInfoEntity = new CardInfo();
        User userEntity = new User();
        userEntity.setId(userId);
        cardInfoEntity.setUser(userEntity);

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
