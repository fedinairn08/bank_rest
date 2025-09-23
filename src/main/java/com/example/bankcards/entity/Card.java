package com.example.bankcards.entity;

import com.example.bankcards.enums.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "cards")
public class Card extends AbstractEntity {

    @Column(name = "number", nullable = false, unique = true, length = 512)
    private String number; // Зашифрованный номер

    @Column(name = "card_holder", nullable = false)
    private String cardHolder;

    @Column(name = "expiry", nullable = false)
    private LocalDate expiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}


