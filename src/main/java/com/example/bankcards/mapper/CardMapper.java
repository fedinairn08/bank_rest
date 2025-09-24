package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardDTOResponse;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(source = "owner.id", target = "userId")
    @Mapping(source = "expiry", target = "expirationDate")
    CardDTOResponse cardToCardDTOResponse(Card card);
}
