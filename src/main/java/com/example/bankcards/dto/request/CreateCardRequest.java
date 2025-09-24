package com.example.bankcards.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCardRequest {
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid card number format")
    private String cardNumber;

    private String cardHolder;

    private LocalDate expirationDate;

    private Long userId;
}
