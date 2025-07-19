package com.example.userservice.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.userservice.repositories.CardInfoRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.mappers.CardInfoMapper;
import com.example.userservice.entities.User;
import com.example.userservice.exceptions.ConflictException;
import com.example.userservice.exceptions.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

@Service
public class CardInfoService {

    private CardInfoRepository cardInfoRepository;
    private UserRepository userRepository;
    private CardInfoMapper cardInfoMapper;
    private CacheManager cacheManager;

    public CardInfoService(CardInfoRepository cardInfoRepository, UserRepository userRepository, CardInfoMapper cardInfoMapper, CacheManager cacheManager) {
        this.cardInfoRepository = cardInfoRepository;
        this.userRepository = userRepository;
        this.cardInfoMapper = cardInfoMapper;
        this.cacheManager = cacheManager;
    }

    @CacheEvict(value = "users", key = "#cardInfoRequestDTO.userId")
    public CardInfoResponseDTO createCardInfo(CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoMapper.toEntity(cardInfoRequestDTO);
        if (cardInfoRepository.existsByNumber(cardInfo.getNumber())) {
            throw new ConflictException(
                String.format(ConflictException.CARD_INFO_ALREADY_EXISTS, cardInfo.getNumber())
            );
        }
        User user = userRepository.findById(cardInfoRequestDTO.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));
        cardInfo.setUser(user);
        CardInfo savedCardInfo = cardInfoRepository.save(cardInfo);
        return cardInfoMapper.toDTO(savedCardInfo);
    }

    public CardInfoResponseDTO getCardInfoById(UUID id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        return cardInfoMapper.toDTO(cardInfo);
    }

    public List<CardInfoResponseDTO> getCardInfosByIds(List<UUID> ids) {
        List<CardInfo> cardInfos = cardInfoRepository.findCardsByIds(ids);
        return cardInfos.stream()
            .map(cardInfoMapper::toDTO)
            .toList();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#cardInfoRequestDTO.userId")
    public CardInfoResponseDTO updateCardInfo(UUID id, CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        if (!cardInfo.getNumber().equals(cardInfoRequestDTO.getNumber()) && cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())) {
            throw new ConflictException(
                String.format(ConflictException.CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())
            );
        }
        if (!userRepository.existsById(cardInfoRequestDTO.getUserId())) {
            throw new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND);
        }

        cardInfoMapper.updateEntity(cardInfoRequestDTO, cardInfo);
        CardInfo updatedCardInfo = cardInfoRepository.save(cardInfo);
        return cardInfoMapper.toDTO(updatedCardInfo);
    } 

    @Transactional
    public void deleteCardInfo(UUID id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        UUID userId = cardInfo.getUser().getId();
        cardInfoRepository.delete(cardInfo);
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
