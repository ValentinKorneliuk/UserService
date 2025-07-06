package com.example.userservice.services;

import java.util.List;
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

    @Transactional
    @CacheEvict(value = "users", key = "#cardInfoRequestDTO.userId")
    public CardInfoResponseDTO createCardInfo(CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoMapper.toEntity(cardInfoRequestDTO);
        if (cardInfoRepository.existsByNumber(cardInfo.getNumber())) {
            throw new ConflictException("Card info with number " + cardInfo.getNumber() + " already exists");
        }
        User user = userRepository.findById(cardInfoRequestDTO.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        cardInfo.setUser(user);
        CardInfo savedCardInfo = cardInfoRepository.save(cardInfo);
        return cardInfoMapper.toDTO(savedCardInfo);
    }

    public CardInfoResponseDTO getCardInfoById(Long id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card info not found"));
        return cardInfoMapper.toDTO(cardInfo);
    }

    public List<CardInfoResponseDTO> getCardInfosByIds(List<Long> ids) {
        List<CardInfo> cardInfos = cardInfoRepository.findCardsByIds(ids);
        return cardInfos.stream()
            .map(cardInfoMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "users", key = "#cardInfoRequestDTO.userId")
    public CardInfoResponseDTO updateCardInfo(Long id, CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card info not found"));
        cardInfoMapper.updateEntity(cardInfoRequestDTO, cardInfo);
        CardInfo updatedCardInfo = cardInfoRepository.save(cardInfo);
        return cardInfoMapper.toDTO(updatedCardInfo);
    } 

    @Transactional
    public void deleteCardInfo(Long id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card info not found"));
        Long userId = cardInfo.getUser().getId();
        cardInfoRepository.delete(cardInfo);
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
