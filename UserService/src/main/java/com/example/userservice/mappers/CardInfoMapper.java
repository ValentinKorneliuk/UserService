package com.example.userservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.entities.CardInfo;

@Mapper(componentModel = "spring")
public interface CardInfoMapper {
    CardInfo toEntity(CardInfoRequestDTO dto);
    CardInfoResponseDTO toDTO(CardInfo entity);
    void updateEntity(CardInfoRequestDTO dto, @MappingTarget CardInfo entity);
}
