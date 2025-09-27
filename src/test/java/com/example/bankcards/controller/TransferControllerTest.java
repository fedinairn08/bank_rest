package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.response.TransferDTOResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Тесты для TransferController")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferService transferService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private TransferMapper transferMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transfer testTransfer;
    private TransferDTOResponse testTransferDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setNumber("encrypted1234567890123456");
        fromCard.setMaskedNumber("****-****-****-1234");
        fromCard.setCardHolder("Test User");
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setBalance(BigDecimal.valueOf(1000.00));
        fromCard.setOwner(testUser);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setNumber("encrypted9876543210987654");
        toCard.setMaskedNumber("****-****-****-5678");
        toCard.setCardHolder("Test User");
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setBalance(BigDecimal.valueOf(500.00));
        toCard.setOwner(testUser);

        testTransfer = new Transfer();
        testTransfer.setId(1L);
        testTransfer.setFromCard(fromCard);
        testTransfer.setToCard(toCard);
        testTransfer.setAmount(BigDecimal.valueOf(100.00));
        testTransfer.setTransferDate(LocalDateTime.now());

        testTransferDTO = new TransferDTOResponse();
        testTransferDTO.setId(1L);
        testTransferDTO.setFromCardMaskedNumber("****-****-****-1234");
        testTransferDTO.setToCardMaskedNumber("****-****-****-5678");
        testTransferDTO.setAmount(BigDecimal.valueOf(100.00));
        testTransferDTO.setTransferDate(LocalDateTime.now());
        testTransferDTO.setFromUserId(1L);
        testTransferDTO.setToUserId(1L);
    }

    @Test
    @DisplayName("Должен успешно получить перевод по ID")
    void getTransfer_ShouldReturnTransfer_WhenValidId() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(transferService.getTransferById(1L, 1L)).thenReturn(testTransfer);
        when(transferMapper.transferToTransferDTOResponse(testTransfer)).thenReturn(testTransferDTO);

        // When & Then
        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fromCardMaskedNumber").value("****-****-****-1234"))
                .andExpect(jsonPath("$.toCardMaskedNumber").value("****-****-****-5678"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.fromUserId").value(1L))
                .andExpect(jsonPath("$.toUserId").value(1L));

        verify(transferService).getTransferById(1L, 1L);
        verify(transferMapper).transferToTransferDTOResponse(testTransfer);
    }

    @Test
    @DisplayName("Должен вернуть ошибку 404 при несуществующем переводе")
    void getTransfer_ShouldReturnNotFound_WhenTransferNotFound() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(transferService.getTransferById(999L, 1L))
                .thenThrow(new com.example.bankcards.exception.ResourceNotFoundException("Transfer not found"));

        // When & Then
        mockMvc.perform(get("/api/transfers/999"))
                .andExpect(status().isNotFound());

        verify(transferService).getTransferById(999L, 1L);
        verify(transferMapper, never()).transferToTransferDTOResponse(any());
    }

    @Test
    @DisplayName("Должен вернуть ошибку 403 при попытке доступа к чужому переводу")
    void getTransfer_ShouldReturnForbidden_WhenAccessDenied() throws Exception {
        // Given
        when(securityUtils.getCurrentUserId()).thenReturn(2L);
        when(transferService.getTransferById(1L, 2L))
                .thenThrow(new com.example.bankcards.exception.AccessDeniedException("Access denied to view this transfer"));

        // When & Then
        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isForbidden());

        verify(transferService).getTransferById(1L, 2L);
        verify(transferMapper, never()).transferToTransferDTOResponse(any());
    }

    @Test
    @DisplayName("Должен успешно получить все переводы (админ)")
    void getAllTransfers_ShouldReturnAllTransfers_WhenAdmin() throws Exception {
        // Given
        List<Transfer> transfers = List.of(testTransfer);
        Page<Transfer> transferPage = new PageImpl<>(transfers, PageRequest.of(0, 20), 1);

        when(transferService.getAllTransfers(any(Pageable.class))).thenReturn(transferPage);
        when(transferMapper.transferToTransferDTOResponse(testTransfer)).thenReturn(testTransferDTO);

        // When & Then
        mockMvc.perform(get("/api/transfers/admin/all")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(transferService).getAllTransfers(any(Pageable.class));
        verify(transferMapper).transferToTransferDTOResponse(testTransfer);
    }

    @Test
    @DisplayName("Должен успешно получить все переводы с пагинацией")
    void getAllTransfers_ShouldReturnPaginatedTransfers_WhenValidPagination() throws Exception {
        // Given
        List<Transfer> transfers = List.of(testTransfer);
        Page<Transfer> transferPage = new PageImpl<>(transfers, PageRequest.of(0, 10), 1);

        when(transferService.getAllTransfers(any(Pageable.class))).thenReturn(transferPage);
        when(transferMapper.transferToTransferDTOResponse(testTransfer)).thenReturn(testTransferDTO);

        // When & Then
        mockMvc.perform(get("/api/transfers/admin/all")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(transferService).getAllTransfers(any(Pageable.class));
        verify(transferMapper).transferToTransferDTOResponse(testTransfer);
    }

    @Test
    @DisplayName("Должен вернуть пустую страницу при отсутствии переводов")
    void getAllTransfers_ShouldReturnEmptyPage_WhenNoTransfers() throws Exception {
        // Given
        Page<Transfer> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(transferService.getAllTransfers(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/transfers/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(transferService).getAllTransfers(any(Pageable.class));
        verify(transferMapper, never()).transferToTransferDTOResponse(any());
    }

    @Test
    @DisplayName("Должен обработать ошибку валидации параметров пагинации")
    void getAllTransfers_ShouldHandleInvalidPagination_WhenInvalidParams() throws Exception {
        // Given
        when(transferService.getAllTransfers(any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Invalid pagination parameters"));

        // When & Then
        mockMvc.perform(get("/api/transfers/admin/all")
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(transferService).getAllTransfers(any(Pageable.class));
    }
}
