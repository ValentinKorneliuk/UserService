package com.example.userservice.mappers;

import com.example.userservice.dto.CardInfoRequestDTO;
import com.example.userservice.dto.CardInfoResponseDTO;
import com.example.userservice.entities.CardInfo;
import com.example.userservice.entities.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T15:40:23+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class CardInfoMapperImpl extends CardInfoMapper {

    @Override
    public CardInfo toEntity(CardInfoRequestDTO dto) {
        cleanCardNumber( dto );

        if ( dto == null ) {
            return null;
        }

        CardInfo.CardInfoBuilder cardInfo = CardInfo.builder();

        cardInfo.number( dto.getNumber() );
        cardInfo.holder( dto.getHolder() );
        cardInfo.expirationDate( dto.getExpirationDate() );

        return cardInfo.build();
    }

    @Override
    public CardInfoResponseDTO toDTO(CardInfo entity) {
        if ( entity == null ) {
            return null;
        }

        CardInfoResponseDTO.CardInfoResponseDTOBuilder cardInfoResponseDTO = CardInfoResponseDTO.builder();

        cardInfoResponseDTO.userId( entityUserId( entity ) );
        cardInfoResponseDTO.id( entity.getId() );
        cardInfoResponseDTO.number( entity.getNumber() );
        cardInfoResponseDTO.holder( entity.getHolder() );
        cardInfoResponseDTO.expirationDate( entity.getExpirationDate() );

        return cardInfoResponseDTO.build();
    }

    @Override
    public void updateEntity(CardInfoRequestDTO dto, CardInfo entity) {
        cleanCardNumber( dto );

        if ( dto == null ) {
            return;
        }

        entity.setNumber( dto.getNumber() );
        entity.setHolder( dto.getHolder() );
        entity.setExpirationDate( dto.getExpirationDate() );
    }

    private Long entityUserId(CardInfo cardInfo) {
        if ( cardInfo == null ) {
            return null;
        }
        User user = cardInfo.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
