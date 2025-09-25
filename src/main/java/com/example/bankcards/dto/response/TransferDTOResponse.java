package com.example.bankcards.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferDTOResponse {
    private Long id;
    private String fromCardMaskedNumber;
    private String toCardMaskedNumber;
    private BigDecimal amount;
    private LocalDateTime transferDate;
    private String description;
    private Long fromUserId;
    private Long toUserId;
}
