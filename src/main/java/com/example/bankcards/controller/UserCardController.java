package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardDTOResponse;
import com.example.bankcards.dto.response.TransferDTOResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/user/cards")
@RequiredArgsConstructor
@Secured("USER")
public class UserCardController {

    private final CardService cardService;
    private final TransferService transferService;
    private final SecurityUtils securityUtils;
    private final CardMapper cardMapper;
    private final TransferMapper transferMapper;

    // Пользователь запрашивает блокировку карты
    @PostMapping("/{cardId}/request-block")
    public ResponseEntity<CardDTOResponse> requestCardBlock(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Card card = cardService.requestCardBlock(cardId, currentUserId);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.ok(cardDTO);
    }

    // Пользователь делает переводы между своими картами
    @PostMapping("/transfer")
    public ResponseEntity<TransferDTOResponse> transferBetweenMyCards(
            @Valid @RequestBody TransferRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Transfer transfer = transferService.transferBetweenUserCards(
                request.getFromCardId(),
                request.getToCardId(),
                request.getAmount(),
                currentUserId
        );
        TransferDTOResponse transferDTO = transferMapper.transferToTransferDTOResponse(transfer);
        return ResponseEntity.ok(transferDTO);
    }

    // Пользователь смотрит баланс конкретной карты
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getCardBalance(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        BigDecimal balance = cardService.getCardBalance(cardId, currentUserId);
        return ResponseEntity.ok(balance);
    }

    // Пользователь смотрит общий баланс всех своих карт
    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        Long currentUserId = securityUtils.getCurrentUserId();
        BigDecimal totalBalance = cardService.getUserTotalBalance(currentUserId);
        return ResponseEntity.ok(totalBalance);
    }

    // История переводов пользователя
    @GetMapping("/transfers")
    public ResponseEntity<Page<TransferDTOResponse>> getUserTransfers(
            @PageableDefault(sort = "transferDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Page<Transfer> transfers = transferService.getUserTransfers(currentUserId, pageable);
        Page<TransferDTOResponse> transferDTOs = transfers.map(transferMapper::transferToTransferDTOResponse);
        return ResponseEntity.ok(transferDTOs);
    }

    // Активные карты пользователя
    @GetMapping("/active")
    public ResponseEntity<List<CardDTOResponse>> getActiveCards() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Card> activeCards = cardService.getUserActiveCards(currentUserId);
        List<CardDTOResponse> cardDTOs = activeCards.stream()
                .map(cardMapper::cardToCardDTOResponse)
                .toList();
        return ResponseEntity.ok(cardDTOs);
    }

    // Заблокированные карты пользователя
    @GetMapping("/blocked")
    public ResponseEntity<List<CardDTOResponse>> getBlockedCards() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Card> blockedCards = cardService.getUserBlockedCards(currentUserId);
        List<CardDTOResponse> cardDTOs = blockedCards.stream()
                .map(cardMapper::cardToCardDTOResponse)
                .toList();
        return ResponseEntity.ok(cardDTOs);
    }
}
