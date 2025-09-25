package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public Transfer transferBetweenUserCards(Long fromCardId, Long toCardId, BigDecimal amount, Long userId) {
        Card fromCard = cardRepository.findByIdAndOwnerId(fromCardId, userId)
                .orElseThrow(() -> new RuntimeException("Source card not found or access denied"));

        Card toCard = cardRepository.findByIdAndOwnerId(toCardId, userId)
                .orElseThrow(() -> new RuntimeException("Target card not found or access denied"));

        return performTransfer(fromCard, toCard, amount);
    }

    private Transfer performTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        validateTransfer(fromCard, toCard, amount);

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);
        transfer.setTransferDate(LocalDateTime.now());

        return transferRepository.save(transfer);
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Source card is not active");
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Target card is not active");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds on source card");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new RuntimeException("Transfer amount exceeds maximum limit");
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new RuntimeException("Cannot transfer to the same card");
        }
    }

    @Transactional(readOnly = true)
    public Page<Transfer> getUserTransfers(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return transferRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Transfer getTransferById(Long transferId, Long userId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        if (!isUserInvolvedInTransfer(transfer, userId) && !isAdmin(userId)) {
            throw new RuntimeException("Access denied to view this transfer");
        }

        return transfer;
    }

    @Transactional(readOnly = true)
    public Page<Transfer> getAllTransfers(Pageable pageable) {
        return transferRepository.findAll(pageable);
    }


    private boolean isUserInvolvedInTransfer(Transfer transfer, Long userId) {
        Long fromUserId = transfer.getFromCard().getOwner().getId();
        Long toUserId = transfer.getToCard().getOwner().getId();
        return fromUserId.equals(userId) || toUserId.equals(userId);
    }

    private boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ADMIN"));
    }
}
