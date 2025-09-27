package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.UserDTOResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.RoleName;
import com.example.bankcards.mapper.UserMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserManagementController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=testSecretKeyThatIsLongEnoughForHS512Algorithm",
    "jwt.expiration=86400000"
})
@DisplayName("Тесты для UserManagementController")
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserDTOResponse testUserDTO;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRoles(Set.of(userRole));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testUserDTO = new UserDTOResponse();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setRoles(Set.of(RoleName.USER));
        testUserDTO.setCreatedAt(LocalDateTime.now());
        testUserDTO.setUpdatedAt(LocalDateTime.now());

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setPassword("password123");
        createUserRequest.setRole(RoleName.USER);

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUsername("updateduser");
        updateUserRequest.setPassword("newpassword123");
        updateUserRequest.setRole(RoleName.ADMIN);
    }

    @Test
    @DisplayName("Должен успешно создать пользователя с валидными данными")
    void createUser_ShouldCreateUser_WhenValidData() throws Exception {
        // Given
        when(userService.createUser(anyString(), anyString(), any(RoleName.class))).thenReturn(testUser);
        when(userMapper.userToUserDTOResponse(testUser)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(userService).createUser("newuser", "password123", RoleName.USER);
        verify(userMapper).userToUserDTOResponse(testUser);
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при невалидных данных создания пользователя")
    void createUser_ShouldReturnValidationError_WhenInvalidData() throws Exception {
        // Given
        createUserRequest.setUsername(""); // Пустое имя пользователя
        createUserRequest.setPassword(""); // Пустой пароль

        // When & Then
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(anyString(), anyString(), any(RoleName.class));
    }

    @Test
    @DisplayName("Должен успешно получить пользователя по ID")
    void getUserById_ShouldReturnUser_WhenValidId() throws Exception {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(userMapper.userToUserDTOResponse(testUser)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(userService).getUserById(1L);
        verify(userMapper).userToUserDTOResponse(testUser);
    }

    @Test
    @DisplayName("Должен успешно обновить пользователя")
    void updateUser_ShouldUpdateUser_WhenValidData() throws Exception {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");

        UserDTOResponse updatedUserDTO = new UserDTOResponse();
        updatedUserDTO.setId(1L);
        updatedUserDTO.setUsername("updateduser");

        when(userService.updateUser(anyLong(), any(UpdateUserRequest.class))).thenReturn(updatedUser);
        when(userMapper.userToUserDTOResponse(updatedUser)).thenReturn(updatedUserDTO);

        // When & Then
        mockMvc.perform(put("/api/admin/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"));

        verify(userService).updateUser(1L, updateUserRequest);
        verify(userMapper).userToUserDTOResponse(updatedUser);
    }

    @Test
    @DisplayName("Должен вернуть ошибку валидации при невалидных данных обновления")
    void updateUser_ShouldReturnValidationError_WhenInvalidData() throws Exception {
        // Given
        updateUserRequest.setUsername("ab"); // Слишком короткое имя
        updateUserRequest.setPassword("123"); // Слишком короткий пароль

        // When & Then
        mockMvc.perform(put("/api/admin/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(anyLong(), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Должен успешно удалить пользователя")
    void deleteUser_ShouldDeleteUser_WhenValidId() throws Exception {
        // Given
        doNothing().when(userService).deleteUser(1L);

        // When & Then
        mockMvc.perform(delete("/api/admin/users/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("Должен успешно получить всех пользователей с пагинацией")
    void getAllUsers_ShouldReturnUsers_WithPagination() throws Exception {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 20), 1);

        when(userService.getAllUsers(any())).thenReturn(userPage);
        when(userMapper.userToUserDTOResponse(testUser)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).getAllUsers(any());
    }

    @Test
    @DisplayName("Должен успешно получить пользователей по роли")
    void getUsersByRole_ShouldReturnUsers_WhenValidRole() throws Exception {
        // Given
        when(userService.getUsersByRole(RoleName.USER)).thenReturn(List.of(testUser));
        when(userMapper.userToUserDTOResponse(testUser)).thenReturn(testUserDTO);

        // When & Then
        mockMvc.perform(get("/api/admin/users/role/USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].roles[0]").value("USER"));

        verify(userService).getUsersByRole(RoleName.USER);
        verify(userMapper).userToUserDTOResponse(testUser);
    }
}