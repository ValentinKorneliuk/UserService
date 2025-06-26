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

@Service
public class CardInfoService {

    private CardInfoRepository cardInfoRepository;
    private UserRepository userRepository;
    private CardInfoMapper cardInfoMapper;

    public CardInfoService(CardInfoRepository cardInfoRepository, UserRepository userRepository, CardInfoMapper cardInfoMapper) {
        this.cardInfoRepository = cardInfoRepository;
        this.userRepository = userRepository;
        this.cardInfoMapper = cardInfoMapper;
    }

    @Transactional
    public CardInfoResponseDTO createCardInfo(CardInfoRequestDTO cardInfoRequestDTO) {
        User user = userRepository.findById(cardInfoRequestDTO.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (cardInfoRepository.existsByNumber(cardInfoRequestDTO.getNumber())) {
            throw new ConflictException("Card info with number " + cardInfoRequestDTO.getNumber() + " already exists");
        }
        CardInfo cardInfo = cardInfoMapper.toEntity(cardInfoRequestDTO);
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
        List<CardInfo> cardInfos = cardInfoRepository.findAllById(ids);
        return cardInfos.stream()
            .map(cardInfoMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public CardInfoResponseDTO updateCardInfo(Long id, CardInfoRequestDTO cardInfoRequestDTO) {
        CardInfo cardInfo = cardInfoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Card info not found"));
        cardInfoMapper.updateEntity(cardInfoRequestDTO, cardInfo);
        CardInfo updatedCardInfo = cardInfoRepository.save(cardInfo);
        return cardInfoMapper.toDTO(updatedCardInfo);
    } 

    @Transactional
    public void deleteCardInfo(Long id) {
        cardInfoRepository.deleteById(id);
    }
}
