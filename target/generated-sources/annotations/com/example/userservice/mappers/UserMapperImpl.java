package com.example.userservice.mappers;

import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.dto.UserRequestDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-14T12:24:12+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.name( dto.getName() );
        user.surname( dto.getSurname() );
        user.birthDate( dto.getBirthDate() );
        user.email( dto.getEmail() );

        return user.build();
    }

    @Override
    public UserResponseDTO toDTO(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserResponseDTO.UserResponseDTOBuilder userResponseDTO = UserResponseDTO.builder();

        userResponseDTO.id( entity.getId() );
        userResponseDTO.name( entity.getName() );
        userResponseDTO.surname( entity.getSurname() );
        userResponseDTO.birthDate( entity.getBirthDate() );
        userResponseDTO.email( entity.getEmail() );
        userResponseDTO.cardInfos( cardInfoListToCardInfoResponseDTOList( entity.getCardInfos() ) );

        return userResponseDTO.build();
    }

    @Override
    public void updateEntity(UserRequestDTO dto, User entity) {
        if ( dto == null ) {
            return;
        }

        entity.setName( dto.getName() );
        entity.setSurname( dto.getSurname() );
        entity.setBirthDate( dto.getBirthDate() );
        entity.setEmail( dto.getEmail() );
    }

    protected CardInfoResponseDTO cardInfoToCardInfoResponseDTO(CardInfo cardInfo) {
        if ( cardInfo == null ) {
            return null;
        }

        CardInfoResponseDTO.CardInfoResponseDTOBuilder cardInfoResponseDTO = CardInfoResponseDTO.builder();

        cardInfoResponseDTO.id( cardInfo.getId() );
        cardInfoResponseDTO.number( cardInfo.getNumber() );
        cardInfoResponseDTO.holder( cardInfo.getHolder() );
        cardInfoResponseDTO.expirationDate( cardInfo.getExpirationDate() );

        return cardInfoResponseDTO.build();
    }

    protected List<CardInfoResponseDTO> cardInfoListToCardInfoResponseDTOList(List<CardInfo> list) {
        if ( list == null ) {
            return null;
        }

        List<CardInfoResponseDTO> list1 = new ArrayList<CardInfoResponseDTO>( list.size() );
        for ( CardInfo cardInfo : list ) {
            list1.add( cardInfoToCardInfoResponseDTO( cardInfo ) );
        }

        return list1;
    }
}
