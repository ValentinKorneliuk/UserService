package com.example.userservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeforeMapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Mapping;

import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.entities.CardInfo;

import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CardInfoMapper {
    public abstract CardInfo toEntity(CardInfoRequestDTO dto);

    @Mapping(target = "userId", source = "user.id")
    public abstract CardInfoResponseDTO toDTO(CardInfo entity);

    public abstract void updateEntity(CardInfoRequestDTO dto, @MappingTarget CardInfo entity);

    @BeforeMapping
    public void cleanCardNumber(CardInfoRequestDTO dto) {
        Optional.ofNullable(dto.getNumber())
                .map(n -> n.replaceAll("[\\s-]", ""))
                .ifPresent(dto::setNumber);
    }
}