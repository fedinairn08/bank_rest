package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.UpdateCardRequest;
import com.example.bankcards.dto.response.CardDTOResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    private final SecurityUtils securityUtils;

    private final CardMapper cardMapper;

    // Создать карту
    @PostMapping
    @Secured("ADMIN")
    public ResponseEntity<CardDTOResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.createCard(request);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardDTO);
    }

    // Получить карту по ID
    @GetMapping("/{cardId}")
    @Secured("ADMIN")
    public ResponseEntity<CardDTOResponse> getCard(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Card card = cardService.getCardById(cardId, currentUserId);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.ok(cardDTO);
    }

    // Все карты текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<Page<CardDTOResponse>> getMyCards(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Page<Card> cards = cardService.getUserCards(currentUserId, pageable);
        Page<CardDTOResponse> cardDTOs = cards.map(cardMapper::cardToCardDTOResponse);
        return ResponseEntity.ok(cardDTOs);
    }

    // Все карты
    @GetMapping("/admin/all")
    @Secured("ADMIN")
    public ResponseEntity<Page<CardDTOResponse>> getAllCards(
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Page<Card> cards = cardService.getAllCards(pageable, currentUserId);
        Page<CardDTOResponse> cardDTOs = cards.map(cardMapper::cardToCardDTOResponse);
        return ResponseEntity.ok(cardDTOs);
    }

    // Обновить карту
    @PutMapping("/{cardId}")
    @Secured("ADMIN")
    public ResponseEntity<CardDTOResponse> updateCard(@PathVariable Long cardId,
                                              @Valid @RequestBody UpdateCardRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Card card = cardService.updateCard(cardId, request, currentUserId);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.ok(cardDTO);
    }

    // Заблокировать карту
    @PostMapping("/{cardId}/block")
    @Secured("ADMIN")
    public ResponseEntity<CardDTOResponse> blockCard(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Card card = cardService.blockCard(cardId, currentUserId);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.ok(cardDTO);
    }

    // Активировать карту
    @PostMapping("/{cardId}/activate")
    @Secured("ADMIN")
    public ResponseEntity<CardDTOResponse> activateCard(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Card card = cardService.activateCard(cardId, currentUserId);
        CardDTOResponse cardDTO = cardMapper.cardToCardDTOResponse(card);
        return ResponseEntity.ok(cardDTO);
    }

    // Удалить карту
    @DeleteMapping("/{cardId}")
    @Secured("ADMIN")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        cardService.deleteCard(cardId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
