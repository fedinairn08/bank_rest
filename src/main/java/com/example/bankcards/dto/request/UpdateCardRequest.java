package com.example.bankcards.dto.request;

import com.example.bankcards.enums.CardStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCardRequest {
    @Size(min = 2, max = 100, message = "Card holder name must be between 2 and 100 characters")
    private String cardHolder;
    
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;
    
    private CardStatus status;
}
