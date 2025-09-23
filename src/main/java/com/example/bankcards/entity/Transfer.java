package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "transfers")
public class Transfer extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard;

    @ManyToOne
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_date", nullable = false)
    private OffsetDateTime transferDate = OffsetDateTime.now();
}


