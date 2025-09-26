package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для TransferService")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transfer testTransfer;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setOwner(testUser);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setOwner(testUser);

        testTransfer = new Transfer();
        testTransfer.setId(1L);
        testTransfer.setFromCard(fromCard);
        testTransfer.setToCard(toCard);
        testTransfer.setAmount(new BigDecimal("100.00"));
        testTransfer.setTransferDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Должен успешно выполнить перевод между картами пользователя")
    void transferBetweenUserCards_ShouldPerformTransfer_WhenValidData() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenReturn(fromCard, toCard);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        // When
        Transfer result = transferService.transferBetweenUserCards(1L, 2L, amount, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getFromCard()).isEqualTo(fromCard);
        assertThat(result.getToCard()).isEqualTo(toCard);
        
        // Проверяем, что балансы обновились
        assertThat(fromCard.getBalance()).isEqualTo(new BigDecimal("900.00"));
        assertThat(toCard.getBalance()).isEqualTo(new BigDecimal("600.00"));
        
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при переводе с несуществующей карты")
    void transferBetweenUserCards_ShouldThrowException_WhenFromCardNotFound() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        when(cardRepository.findByIdAndOwnerId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(999L, 2L, amount, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Source card not found or access denied");
        verify(cardRepository).findByIdAndOwnerId(999L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при переводе на несуществующую карту")
    void transferBetweenUserCards_ShouldThrowException_WhenToCardNotFound() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 999L, amount, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Target card not found or access denied");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(999L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при переводе с неактивной карты")
    void transferBetweenUserCards_ShouldThrowException_WhenFromCardInactive() {
        // Given
        fromCard.setStatus(CardStatus.BLOCKED);
        BigDecimal amount = new BigDecimal("100.00");
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 2L, amount, 1L))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Source card is not active");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при переводе на неактивную карту")
    void transferBetweenUserCards_ShouldThrowException_WhenToCardInactive() {
        // Given
        toCard.setStatus(CardStatus.BLOCKED);
        BigDecimal amount = new BigDecimal("100.00");
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 2L, amount, 1L))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Target card is not active");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при недостаточном балансе")
    void transferBetweenUserCards_ShouldThrowException_WhenInsufficientFunds() {
        // Given
        BigDecimal amount = new BigDecimal("1500.00"); // Больше чем баланс карты
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 2L, amount, 1L))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Insufficient funds on source card");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при отрицательной сумме перевода")
    void transferBetweenUserCards_ShouldThrowException_WhenAmountIsNegative() {
        // Given
        BigDecimal amount = new BigDecimal("-100.00");
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 2L, amount, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transfer amount must be positive");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при нулевой сумме перевода")
    void transferBetweenUserCards_ShouldThrowException_WhenAmountIsZero() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;
        when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> transferService.transferBetweenUserCards(1L, 2L, amount, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transfer amount must be positive");
        verify(cardRepository).findByIdAndOwnerId(1L, 1L);
        verify(cardRepository).findByIdAndOwnerId(2L, 1L);
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен вернуть страницу переводов пользователя")
    void getUserTransfers_ShouldReturnUserTransfers() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<Transfer> expectedPage = new PageImpl<>(List.of(testTransfer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transferRepository.findByUser(testUser, pageable)).thenReturn(expectedPage);

        // When
        Page<Transfer> result = transferService.getUserTransfers(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getAmount()).isEqualTo(new BigDecimal("100.00"));
        verify(userRepository).findById(1L);
        verify(transferRepository).findByUser(testUser, pageable);
    }

    @Test
    @DisplayName("Должен выбросить исключение при поиске переводов несуществующего пользователя")
    void getUserTransfers_ShouldThrowException_WhenUserNotFound() {
        // Given
        Pageable pageable = mock(Pageable.class);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.getUserTransfers(999L, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        verify(userRepository).findById(999L);
        verify(transferRepository, never()).findByUser(any(), any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при доступе к чужому переводу")
    void getTransferById_ShouldThrowException_WhenAccessDenied() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        
        when(transferRepository.findById(1L)).thenReturn(Optional.of(testTransfer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        // When & Then
        assertThatThrownBy(() -> transferService.getTransferById(1L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to view this transfer");
        verify(transferRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при поиске несуществующего перевода")
    void getTransferById_ShouldThrowException_WhenTransferNotFound() {
        // Given
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.getTransferById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transfer not found");
        verify(transferRepository).findById(999L);
    }

    @Test
    @DisplayName("Должен вернуть все переводы")
    void getAllTransfers_ShouldReturnAllTransfers() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<Transfer> expectedPage = new PageImpl<>(List.of(testTransfer));
        when(transferRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<Transfer> result = transferService.getAllTransfers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getAmount()).isEqualTo(new BigDecimal("100.00"));
        verify(transferRepository).findAll(pageable);
    }
}
