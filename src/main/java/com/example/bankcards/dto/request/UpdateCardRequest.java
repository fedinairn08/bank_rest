package com.example.bankcards.dto.request;

import com.example.bankcards.enums.CardStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCardRequest {
    private String cardHolder;
    private LocalDate expirationDate;
    private CardStatus status;
}
