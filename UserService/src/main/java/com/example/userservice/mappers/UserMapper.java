package com.example.userservice.mappers;

import com.example.userservice.dto.CreateUserRequestDto;
import com.example.userservice.dto.UpdateUserRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CardInfoMapper.class})
public interface UserMapper {

    User toEntity(CreateUserRequestDto dto);

    User toEntity(UpdateUserRequestDTO dto);

    UserResponseDTO toDTO(User entity);

    void updateEntity(UpdateUserRequestDTO dto, @MappingTarget User entity);
}