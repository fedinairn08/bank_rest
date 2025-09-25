package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.TransferDTOResponse;
import com.example.bankcards.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(source = "fromCard.number", target = "fromCardMaskedNumber")
    @Mapping(source = "toCard.number", target = "toCardMaskedNumber")
    @Mapping(source = "fromCard.owner.id", target = "fromUserId")
    @Mapping(source = "toCard.owner.id", target = "toUserId")
    TransferDTOResponse transferToTransferDTOResponse(Transfer transfer);
}
