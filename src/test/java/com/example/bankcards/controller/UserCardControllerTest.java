package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardDTOResponse;
import com.example.bankcards.dto.response.TransferDTOResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserCardController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Тесты для UserCardController")
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private TransferService transferService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private CardMapper cardMapper;

    @MockBean
    private TransferMapper transferMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Card testCard;
    private CardDTOResponse testCardDTO;
    private Transfer testTransfer;
    private TransferDTOResponse testTransferDTO;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCard = new Card();
        testCard.setId(1L);
        testCard.setNumber("encrypted1234567890123456");
        testCard.setMaskedNumber("****-****-****-1234");
        testCard.setCardHolder("Test User");
        testCard.setExpiry(LocalDate.now().plusYears(2));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(BigDecimal.valueOf(1000.00));
        testCard.setOwner(testUser);
        testCard.setCreatedAt(LocalDateTime.now());
        testCard.setUpdatedAt(LocalDateTime.now());

        testCardDTO = new CardDTOResponse();
        testCardDTO.setId(1L);
        testCardDTO.setMaskedNumber("****-****-****-1234");
        testCardDTO.setCardHolder("Test User");
        testCardDTO.setExpirationDate(LocalDate.now().plusYears(2));
        testCardDTO.setBalance(BigDecimal.valueOf(1000.00));
        testCardDTO.setStatus(CardStatus.ACTIVE);
        testCardDTO.setUserId(1L);
        testCardDTO.setCreatedAt(LocalDateTime.now());
        testCardDTO.setUpdatedAt(LocalDateTime.now());

        testTransfer = new Transfer();
        testTransfer.setId(1L);
        testTransfer.setAmount(BigDecimal.valueOf(100.00));
        testTransfer.setTransferDate(LocalDateTime.now());

        testTransferDTO = new TransferDTOResponse();
        testTransferDTO.setId(1L);
        testTransferDTO.setFromCardMaskedNumber("****-****-****-1234");
        testTransferDTO.setToCardMaskedNumber("****-****-****-5678");
        testTransferDTO.setAmount(BigDecimal.valueOf(100.00));
        testTransferDTO.setDescription("Test transfer");
        testTransferDTO.setTransferDate(LocalDateTime.now());
        testTransferDTO.setFromUserId(1L);
        testTransferDTO.setToUserId(1L);

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    @DisplayName("Должен успешно запросить блокировку карты")
    void requestCardBlock_ShouldRequestBlock_WhenValidCardId() throws Exception {
        // Given
        Card blockedCard = new Card();
        blockedCard.setId(1L);
        blockedCard.setStatus(CardStatus.BLOCKED);
        blockedCard.setCardHolder("Test User");

        CardDTOResponse blockedCardDTO = new CardDTOResponse();
        blockedCardDTO.setId(1L);
        blockedCardDTO.setStatus(CardStatus.BLOCKED);
        blockedCardDTO.setCardHolder("Test User");

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.requestCardBlock(1L, 1L)).thenReturn(blockedCard);
        when(cardMapper.cardToCardDTOResponse(blockedCard)).thenReturn(blockedCardDTO);

        // When & Then
        mockMvc.perform(post("/api/user/cards/1/request-block")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).requestCardBlock(1L, 1L);
        verify(cardMapper).cardToCardDTOResponse(blockedCard);
    }

    @Test
    @DisplayName("Должен успешно выполнить перевод между своими картами")
    void transferBetweenMyCards_ShouldTransfer_WhenValidRequest() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(transferService.transferBetweenUserCards(1L, 2L, BigDecimal.valueOf(100.00), 1L))
                .thenReturn(testTransfer);
        when(transferMapper.transferToTransferDTOResponse(testTransfer)).thenReturn(testTransferDTO);

        // When & Then
        mockMvc.perform(post("/api/user/cards/transfer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.description").value("Test transfer"));

        verify(transferService).transferBetweenUserCards(1L, 2L, BigDecimal.valueOf(100.00), 1L);
        verify(transferMapper).transferToTransferDTOResponse(testTransfer);
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при невалидном запросе перевода")
    void transferBetweenMyCards_ShouldReturnValidationError_WhenInvalidRequest() throws Exception {
        // Given
        transferRequest.setAmount(BigDecimal.valueOf(-10.00)); // Невалидная сумма

        // When & Then
        mockMvc.perform(post("/api/user/cards/transfer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transferBetweenUserCards(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("Должен успешно получить баланс конкретной карты")
    void getCardBalance_ShouldReturnBalance_WhenValidCardId() throws Exception {
        // Given
        BigDecimal expectedBalance = BigDecimal.valueOf(1000.00);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardBalance(1L, 1L)).thenReturn(expectedBalance);

        // When & Then
        mockMvc.perform(get("/api/user/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1000.00));

        verify(cardService).getCardBalance(1L, 1L);
    }

    @Test
    @DisplayName("Должен успешно получить общий баланс всех карт пользователя")
    void getTotalBalance_ShouldReturnTotalBalance_WhenUserExists() throws Exception {
        // Given
        BigDecimal expectedTotalBalance = BigDecimal.valueOf(2500.00);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.getUserTotalBalance(1L)).thenReturn(expectedTotalBalance);

        // When & Then
        mockMvc.perform(get("/api/user/cards/total-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2500.00));

        verify(cardService).getUserTotalBalance(1L);
    }

    @Test
    @DisplayName("Должен успешно получить активные карты пользователя")
    void getActiveCards_ShouldReturnActiveCards_WhenUserExists() throws Exception {
        // Given
        List<Card> activeCards = Collections.singletonList(testCard);
        
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.getUserActiveCards(1L)).thenReturn(activeCards);
        when(cardMapper.cardToCardDTOResponse(testCard)).thenReturn(testCardDTO);

        // When & Then
        mockMvc.perform(get("/api/user/cards/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].cardHolder").value("Test User"));

        verify(cardService).getUserActiveCards(1L);
        verify(cardMapper).cardToCardDTOResponse(testCard);
    }

    @Test
    @DisplayName("Должен успешно получить заблокированные карты пользователя")
    void getBlockedCards_ShouldReturnBlockedCards_WhenUserExists() throws Exception {
        // Given
        Card blockedCard = new Card();
        blockedCard.setId(2L);
        blockedCard.setStatus(CardStatus.BLOCKED);
        blockedCard.setCardHolder("Test User");
        blockedCard.setMaskedNumber("****-****-****-5678");

        CardDTOResponse blockedCardDTO = new CardDTOResponse();
        blockedCardDTO.setId(2L);
        blockedCardDTO.setStatus(CardStatus.BLOCKED);
        blockedCardDTO.setCardHolder("Test User");
        blockedCardDTO.setMaskedNumber("****-****-****-5678");

        List<Card> blockedCards = List.of(blockedCard);
        
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.getUserBlockedCards(1L)).thenReturn(blockedCards);
        when(cardMapper.cardToCardDTOResponse(blockedCard)).thenReturn(blockedCardDTO);

        // When & Then
        mockMvc.perform(get("/api/user/cards/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].status").value("BLOCKED"))
                .andExpect(jsonPath("$[0].cardHolder").value("Test User"));

        verify(cardService).getUserBlockedCards(1L);
        verify(cardMapper).cardToCardDTOResponse(blockedCard);
    }
}
