package com.example.bankcards.entity;

import com.example.bankcards.enums.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name = "cards")
public class Card extends AbstractEntity {

    @Column(name = "number", nullable = false, unique = true, length = 512)
    private String number;

    // Срок действия, формат YYYY-MM (YearMonth). Можно хранить как строку YYYY-MM
    @Column(name = "expiry", nullable = false, length = 7)
    private String expiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}


