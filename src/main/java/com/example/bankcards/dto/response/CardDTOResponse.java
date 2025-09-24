package com.example.bankcards.dto.response;

import com.example.bankcards.enums.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CardDTOResponse {
    private Long id;
    private String maskedNumber;
    private String cardHolder;
    private LocalDate expirationDate;
    private BigDecimal balance;
    private CardStatus status;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
