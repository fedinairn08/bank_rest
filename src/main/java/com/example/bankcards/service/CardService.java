package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    private final EncryptionUtils encryptionUtils;

    private final UserService userService;

    private final UserRepository userRepository;

    public Card createCard(CreateCardRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        String encryptedCardNumber = encryptionUtils.encrypt(request.getCardNumber());
        String lastFour = request.getCardNumber().substring(request.getCardNumber().length() - 4);

        if (cardRepository.existsByNumber(encryptedCardNumber)) {
            throw new BusinessLogicException("Card with this number already exists");
        }

        Card card = new Card();
        card.setNumber(encryptedCardNumber);
        card.setMaskedNumber("**** **** **** " + lastFour);
        card.setCardHolder(request.getCardHolder());
        card.setExpiry(request.getExpirationDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setOwner(user);

        return cardRepository.save(card);
    }

    public Card getCardById(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(userId) && !userService.isAdmin(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return card;
    }

    public Page<Card> getUserCards(Long userId, Pageable pageable) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cardRepository.findByOwner(user, pageable);
    }

    public Page<Card> getAllCards(Pageable pageable, Long adminUserId) {
        if (!userService.isAdmin(adminUserId)) {
            throw new AccessDeniedException("Access denied. Admin role required.");
        }

        return cardRepository.findAll(pageable);
    }

    public Card updateCard(Long cardId, UpdateCardRequest request, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(userId) && !userService.isAdmin(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.getCardHolder() != null) {
            card.setCardHolder(request.getCardHolder());
        }

        if (request.getExpirationDate() != null) {
            card.setExpiry(request.getExpirationDate());
        }

        if (request.getStatus() != null) {
            card.setStatus(request.getStatus());
        }

        return cardRepository.save(card);
    }

    public Card blockCard(Long cardId, Long userId) {
        return updateCardStatus(cardId, CardStatus.BLOCKED, userId);
    }

    public Card activateCard(Long cardId, Long userId) {
        return updateCardStatus(cardId, CardStatus.ACTIVE, userId);
    }

    public void deleteCard(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(userId) && !userService.isAdmin(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (card.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessLogicException("Cannot delete card with non-zero balance");
        }

        cardRepository.delete(card);
    }

    private Card updateCardStatus(Long cardId, CardStatus status, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(userId) && !userService.isAdmin(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        card.setStatus(status);
        return cardRepository.save(card);
    }

    public Card requestCardBlock(Long cardId, Long userId) {
        Card card = getCardById(cardId, userId);

        if (!card.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only card owner can request block");
        }

        card.setStatus(CardStatus.BLOCKED);

        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCardBalance(Long cardId, Long userId) {
        Card card = getCardById(cardId, userId);
        return card.getBalance();
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Card> userCards = cardRepository.findByOwner(user);

        return userCards.stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<Card> getUserActiveCards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cardRepository.findByOwnerAndStatus(user, CardStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Card> getUserBlockedCards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cardRepository.findByOwnerAndStatus(user, CardStatus.BLOCKED);
    }
}
