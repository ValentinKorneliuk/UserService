package com.example.userservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserRequestDTO dto);
    UserResponseDTO toDTO(User entity);
    void updateEntity(UserRequestDTO dto, @MappingTarget User entity);
}
