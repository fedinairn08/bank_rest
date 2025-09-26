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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CardService")
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EncryptionUtils encryptionUtils;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CreateCardRequest createRequest;
    private UpdateCardRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCard = new Card();
        testCard.setId(1L);
        testCard.setNumber("encryptedCardNumber");
        testCard.setMaskedNumber("**** **** **** 1234");
        testCard.setCardHolder("John Doe");
        testCard.setExpiry(LocalDate.of(2025, 12, 1));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(BigDecimal.ZERO);
        testCard.setOwner(testUser);

        createRequest = new CreateCardRequest();
        createRequest.setUserId(1L);
        createRequest.setCardNumber("1234567890123456");
        createRequest.setCardHolder("John Doe");
        createRequest.setExpirationDate(LocalDate.of(2025, 12, 1));

        updateRequest = new UpdateCardRequest();
        updateRequest.setCardHolder("Jane Doe");
        updateRequest.setExpirationDate(LocalDate.of(2026, 12, 1));
        updateRequest.setStatus(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании карты с существующим номером")
    void createCard_ShouldThrowException_WhenCardNumberExists() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionUtils.encrypt("1234567890123456")).thenReturn("encryptedCardNumber");
        when(cardRepository.existsByNumber("encryptedCardNumber")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> cardService.createCard(createRequest))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Card with this number already exists");
        verify(userService).findById(1L);
        verify(encryptionUtils).encrypt("1234567890123456");
        verify(cardRepository).existsByNumber("encryptedCardNumber");
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен вернуть карту по ID для администратора")
    void getCardById_ShouldReturnCard_WhenUserIsAdmin() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(userService.isAdmin(2L)).thenReturn(true);

        // When
        Card result = cardService.getCardById(1L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(cardRepository).findById(1L);
        verify(userService).isAdmin(2L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при доступе к чужой карте")
    void getCardById_ShouldThrowException_WhenAccessDenied() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(userService.isAdmin(2L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> cardService.getCardById(1L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
        verify(cardRepository).findById(1L);
        verify(userService).isAdmin(2L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при поиске несуществующей карты")
    void getCardById_ShouldThrowException_WhenCardNotFound() {
        // Given
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cardService.getCardById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Card not found with id: 999");
        verify(cardRepository).findById(999L);
    }

    @Test
    @DisplayName("Должен вернуть страницу карт пользователя")
    void getUserCards_ShouldReturnUserCards() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<Card> expectedPage = new PageImpl<>(List.of(testCard));
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwner(testUser, pageable)).thenReturn(expectedPage);

        // When
        Page<Card> result = cardService.getUserCards(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCardHolder()).isEqualTo("John Doe");
        verify(userService).findById(1L);
        verify(cardRepository).findByOwner(testUser, pageable);
    }

    @Test
    @DisplayName("Должен вернуть все карты для администратора")
    void getAllCards_ShouldReturnAllCards_WhenUserIsAdmin() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<Card> expectedPage = new PageImpl<>(List.of(testCard));
        when(userService.isAdmin(1L)).thenReturn(true);
        when(cardRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<Card> result = cardService.getAllCards(pageable, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(userService).isAdmin(1L);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Должен выбросить исключение при получении всех карт не администратором")
    void getAllCards_ShouldThrowException_WhenUserIsNotAdmin() {
        // Given
        Pageable pageable = mock(Pageable.class);
        when(userService.isAdmin(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> cardService.getAllCards(pageable, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied. Admin role required.");
        verify(userService).isAdmin(1L);
        verify(cardRepository, never()).findAll((Pageable) any());
    }

    @Test
    @DisplayName("Должен успешно заблокировать карту владельцем")
    void blockCard_ShouldBlockCard_WhenOwner() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When
        Card result = cardService.blockCard(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).findById(1L);
        verify(cardRepository).save(any(Card.class));
        verify(userService, never()).isAdmin(any());
    }

    @Test
    @DisplayName("Должен успешно заблокировать карту администратором")
    void blockCard_ShouldBlockCard_WhenAdmin() {
        // Given
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        
        Card cardToBlock = new Card();
        cardToBlock.setId(1L);
        cardToBlock.setNumber("encryptedNumber");
        cardToBlock.setMaskedNumber("**** **** **** 1234");
        cardToBlock.setCardHolder("John Doe");
        cardToBlock.setExpiry(LocalDate.of(2025, 12, 1));
        cardToBlock.setStatus(CardStatus.ACTIVE);
        cardToBlock.setBalance(BigDecimal.ZERO);
        cardToBlock.setOwner(testUser);
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardToBlock));
        when(userService.isAdmin(2L)).thenReturn(true);
        when(cardRepository.save(any(Card.class))).thenReturn(cardToBlock);

        // When
        Card result = cardService.blockCard(1L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).findById(1L);
        verify(userService).isAdmin(2L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Должен успешно активировать карту владельцем")
    void activateCard_ShouldActivateCard_WhenOwner() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When
        Card result = cardService.activateCard(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository).findById(1L);
        verify(cardRepository).save(any(Card.class));
        verify(userService, never()).isAdmin(any());
    }

    @Test
    @DisplayName("Должен вернуть общий баланс пользователя")
    void getUserTotalBalance_ShouldReturnTotalBalance() {
        // Given
        Card card1 = new Card();
        card1.setBalance(new BigDecimal("100.00"));
        Card card2 = new Card();
        card2.setBalance(new BigDecimal("200.00"));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwner(testUser)).thenReturn(List.of(card1, card2));

        // When
        BigDecimal result = cardService.getUserTotalBalance(1L);

        // Then
        assertThat(result).isEqualTo(new BigDecimal("300.00"));
        verify(userRepository).findById(1L);
        verify(cardRepository).findByOwner(testUser);
    }

    @Test
    @DisplayName("Должен вернуть активные карты пользователя")
    void getUserActiveCards_ShouldReturnActiveCards() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwnerAndStatus(testUser, CardStatus.ACTIVE)).thenReturn(List.of(testCard));

        // When
        List<Card> result = cardService.getUserActiveCards(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(userRepository).findById(1L);
        verify(cardRepository).findByOwnerAndStatus(testUser, CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Должен вернуть заблокированные карты пользователя")
    void getUserBlockedCards_ShouldReturnBlockedCards() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwnerAndStatus(testUser, CardStatus.BLOCKED)).thenReturn(List.of(testCard));

        // When
        List<Card> result = cardService.getUserBlockedCards(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(userRepository).findById(1L);
        verify(cardRepository).findByOwnerAndStatus(testUser, CardStatus.BLOCKED);
    }
}
