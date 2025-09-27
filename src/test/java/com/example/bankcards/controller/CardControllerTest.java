package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.UpdateCardRequest;
import com.example.bankcards.dto.response.CardDTOResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.CardService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Тесты для CardController")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private CardMapper cardMapper;

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
    private CreateCardRequest createCardRequest;
    private UpdateCardRequest updateCardRequest;

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

        createCardRequest = new CreateCardRequest();
        createCardRequest.setCardNumber("1234567890123456");
        createCardRequest.setCardHolder("Test User");
        createCardRequest.setExpirationDate(LocalDate.now().plusYears(2));
        createCardRequest.setUserId(1L);

        updateCardRequest = new UpdateCardRequest();
        updateCardRequest.setCardHolder("Updated User");
        updateCardRequest.setExpirationDate(LocalDate.now().plusYears(3));
        updateCardRequest.setStatus(CardStatus.ACTIVE);
    }


    @Test
    @DisplayName("Должен успешно создать карту с валидными данными")
    void createCard_ShouldCreateCard_WhenValidData() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(testCard);
        when(cardMapper.cardToCardDTOResponse(testCard)).thenReturn(testCardDTO);

        // When & Then
        mockMvc.perform(post("/api/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cardHolder").value("Test User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).createCard(any(CreateCardRequest.class));
        verify(cardMapper).cardToCardDTOResponse(testCard);
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при невалидном номере карты")
    void createCard_ShouldReturnValidationError_WhenInvalidCardNumber() throws Exception {
        // Given
        createCardRequest.setCardNumber("123"); // Невалидный номер

        // When & Then
        mockMvc.perform(post("/api/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).createCard(any());
    }

    @Test
    @DisplayName("Должен успешно получить карту по ID")
    void getCard_ShouldReturnCard_WhenValidId() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardById(1L, 1L)).thenReturn(testCard);
        when(cardMapper.cardToCardDTOResponse(testCard)).thenReturn(testCardDTO);

        // When & Then
        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cardHolder").value("Test User"));

        verify(cardService).getCardById(1L, 1L);
        verify(cardMapper).cardToCardDTOResponse(testCard);
    }

    @Test
    @DisplayName("Должен успешно заблокировать карту")
    void blockCard_ShouldBlockCard_WhenValidId() throws Exception {
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
        when(cardService.blockCard(1L, 1L)).thenReturn(blockedCard);
        when(cardMapper.cardToCardDTOResponse(blockedCard)).thenReturn(blockedCardDTO);

        // When & Then
        mockMvc.perform(post("/api/cards/1/block")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).blockCard(1L, 1L);
        verify(cardMapper).cardToCardDTOResponse(blockedCard);
    }

    @Test
    @DisplayName("Должен успешно активировать карту")
    void activateCard_ShouldActivateCard_WhenValidId() throws Exception {
        // Given
        Card activatedCard = new Card();
        activatedCard.setId(1L);
        activatedCard.setStatus(CardStatus.ACTIVE);
        activatedCard.setCardHolder("Test User");

        CardDTOResponse activatedCardDTO = new CardDTOResponse();
        activatedCardDTO.setId(1L);
        activatedCardDTO.setStatus(CardStatus.ACTIVE);
        activatedCardDTO.setCardHolder("Test User");

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.activateCard(1L, 1L)).thenReturn(activatedCard);
        when(cardMapper.cardToCardDTOResponse(activatedCard)).thenReturn(activatedCardDTO);

        // When & Then
        mockMvc.perform(post("/api/cards/1/activate")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).activateCard(1L, 1L);
        verify(cardMapper).cardToCardDTOResponse(activatedCard);
    }

    @Test
    @DisplayName("Должен успешно обновить карту")
    void updateCard_ShouldUpdateCard_WhenValidData() throws Exception {
        // Given
        Card updatedCard = new Card();
        updatedCard.setId(1L);
        updatedCard.setCardHolder("Updated User");
        updatedCard.setStatus(CardStatus.ACTIVE);

        CardDTOResponse updatedCardDTO = new CardDTOResponse();
        updatedCardDTO.setId(1L);
        updatedCardDTO.setCardHolder("Updated User");
        updatedCardDTO.setStatus(CardStatus.ACTIVE);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cardService.updateCard(1L, updateCardRequest, 1L)).thenReturn(updatedCard);
        when(cardMapper.cardToCardDTOResponse(updatedCard)).thenReturn(updatedCardDTO);

        // When & Then
        mockMvc.perform(put("/api/cards/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateCardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardHolder").value("Updated User"));

        verify(cardService).updateCard(1L, updateCardRequest, 1L);
        verify(cardMapper).cardToCardDTOResponse(updatedCard);
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при невалидных данных обновления")
    void updateCard_ShouldReturnValidationError_WhenInvalidData() throws Exception {
        // Given
        updateCardRequest.setCardHolder("A"); // Слишком короткое имя

        // When & Then
        mockMvc.perform(put("/api/cards/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateCardRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).updateCard(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("Должен успешно удалить карту")
    void deleteCard_ShouldDeleteCard_WhenValidId() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        doNothing().when(cardService).deleteCard(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/api/cards/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(cardService).deleteCard(1L, 1L);
    }
}
