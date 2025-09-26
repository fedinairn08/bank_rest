package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.request.AuthRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.RoleName;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({CustomUserDetailsService.class, TestSecurityConfig.class})
@DisplayName("Тесты для AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private UserService userService;

    // ДОБАВЬТЕ ЭТОТ МОК
    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setRoles(Collections.singleton(userRole));

        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Должен успешно аутентифицировать пользователя")
    void login_ShouldAuthenticateUser_WhenValidCredentials() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("USER"));

        verify(authenticationManager).authenticate(any());
        verify(tokenProvider).generateToken(authentication);
        verify(userService).findByUsername("testuser");
    }

    @Test
    @DisplayName("Должен вернуть ошибку при отсутствии пользователя")
    void login_ShouldReturnError_WhenUserNotFound() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isNotFound());

        verify(authenticationManager).authenticate(any());
        verify(tokenProvider).generateToken(authentication);
        verify(userService).findByUsername("testuser");
    }

    @Test
    @DisplayName("Должен вернуть ошибку при пустом запросе")
    void login_ShouldReturnError_WhenEmptyRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при null запросе")
    void login_ShouldReturnError_WhenNullRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен успешно зарегистрировать пользователя")
    void register_ShouldRegisterUser_WhenValidData() throws Exception {
        // Given
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.createUser("newuser", "password123", RoleName.USER)).thenReturn(testUser);
        
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        verify(userService).findByUsername("newuser");
        verify(userService).createUser("newuser", "password123", RoleName.USER);
        verify(authenticationManager).authenticate(any());
        verify(tokenProvider).generateToken(authentication);
    }

    @Test
    @DisplayName("Должен вернуть ошибку при регистрации с существующим именем пользователя")
    void register_ShouldReturnError_WhenUsernameExists() throws Exception {
        // Given
        when(userService.findByUsername("existinguser")).thenReturn(Optional.of(testUser));

        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername("existinguser");
        registerRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(userService).findByUsername("existinguser");
        verify(userService, never()).createUser(any(), any(), any());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при пустом запросе регистрации")
    void register_ShouldReturnError_WhenEmptyRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при null запросе регистрации")
    void register_ShouldReturnError_WhenNullRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при неверном JSON")
    void login_ShouldReturnError_WhenInvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при неверном JSON для регистрации")
    void register_ShouldReturnError_WhenInvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }
}
