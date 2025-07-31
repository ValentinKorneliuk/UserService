package com.example.userservice.services;

import java.util.List;
import java.util.UUID;

import dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException;

import static dev.kornliuk.jwtauthstarter.exceptions.ForbiddenAccessException.ACCESS_FORBIDDEN;

import dev.kornliuk.jwtauthstarter.security.UserDetailsProvider;
import lombok.RequiredArgsConstructor;
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
import org.springframework.cache.CacheManager;

@Service
@RequiredArgsConstructor
public class CardInfoService {
    private static final String USERS_CACHE_NAME = "users";

    private final CardInfoRepository cardInfoRepository;
    private final UserRepository userRepository;
    private final CardInfoMapper cardInfoMapper;
    private final CacheManager cacheManager;
    private final UserDetailsProvider userDetailsProvider;

    public CardInfoResponseDTO createCardInfo(CardInfoRequestDTO cardInfoRequestDTO) {
        UUID userId = userDetailsProvider.getUserId();
        CardInfo cardInfo = cardInfoMapper.toEntity(cardInfoRequestDTO);
        if (cardInfoRepository.existsByNumber(cardInfo.getNumber())) {
            throw new ConflictException(
                    String.format(ConflictException.CARD_INFO_ALREADY_EXISTS, cardInfo.getNumber())
            );
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.USER_NOT_FOUND));
        cardInfo.setUser(user);
        CardInfo savedCardInfo = cardInfoRepository.save(cardInfo);

        cacheManager.getCache(USERS_CACHE_NAME).evict(userId);
        return cardInfoMapper.toDTO(savedCardInfo);
    }

    public CardInfoResponseDTO getCardInfoById(UUID id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        if (!isOwner(cardInfo.getUser().getId())) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }

        return cardInfoMapper.toDTO(cardInfo);
    }

    public List<CardInfoResponseDTO> getCardInfosByIds(List<UUID> ids) {
        List<CardInfo> cardInfos = cardInfoRepository.findCardsByIds(ids);
        return cardInfos.stream()
                .map(cardInfoMapper::toDTO)
                .toList();
    }

    @Transactional
    public CardInfoResponseDTO updateCardInfo(UUID id, CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        validateCardNumberUpdate(cardInfo, cardInfoRequestDTO);

        cardInfoMapper.updateEntity(cardInfoRequestDTO, cardInfo);
        CardInfo updatedCardInfo = cardInfoRepository.save(cardInfo);

        cacheManager.getCache(USERS_CACHE_NAME).evict(updatedCardInfo.getUser().getId());
        return cardInfoMapper.toDTO(updatedCardInfo);
    }


    @Transactional
    public void deleteCardInfo(UUID id) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceNotFoundException.CARD_INFO_NOT_FOUND));
        if (!canDeleteCardInfo(cardInfo.getUser().getId())) {
            throw new ForbiddenAccessException(ACCESS_FORBIDDEN);
        }

        UUID userId = cardInfo.getUser().getId();
        cardInfoRepository.delete(cardInfo);
        cacheManager.getCache(USERS_CACHE_NAME).evict(userId);
    }

    private void validateCardNumberUpdate(CardInfo cardInfo, CardInfoRequestDTO cardInfoRequestDTO) {
        if (!cardInfo.getNumber().equals(cardInfoRequestDTO.getNumber()) && cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())) {
            throw new ConflictException(
                    String.format(ConflictException.CARD_INFO_ALREADY_EXISTS, cardInfoRequestDTO.getNumber())
            );
        }
    }

    private boolean isOwner(UUID userId) {
        return userId.equals(userDetailsProvider.getUserId());
    }

    private boolean canDeleteCardInfo(UUID userId) {
        boolean isOwner = userId.equals(userDetailsProvider.getUserId());
        boolean isAdmin = userDetailsProvider.hasRole("ADMIN");

        return isOwner || isAdmin;
    }
}