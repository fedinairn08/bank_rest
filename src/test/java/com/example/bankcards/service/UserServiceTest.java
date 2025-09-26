package com.example.bankcards.service;

import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.RoleName;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.repository.RoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(RoleName.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setRoles(Collections.singleton(testRole));

        updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("newusername");
        updateRequest.setPassword("newpassword");
    }

    @Test
    @DisplayName("Должен найти пользователя по имени пользователя")
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional когда пользователь не найден")
    void findByUsername_ShouldReturnEmpty_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Должен найти пользователя по ID")
    void findById_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Должен выбросить исключение когда пользователь не найден по ID")
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Должен успешно создать пользователя")
    void createUser_ShouldCreateUser_WhenValidData() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser("newuser", "password123", RoleName.USER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).existsByUsername("newuser");
        verify(roleRepository).findByName(RoleName.USER);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании пользователя с существующим именем")
    void createUser_ShouldThrowException_WhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser("existinguser", "password123", RoleName.USER))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Username already exists");
        verify(userRepository).existsByUsername("existinguser");
        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить исключение при создании пользователя с несуществующей ролью")
    void createUser_ShouldThrowException_WhenRoleNotFound() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.createUser("newuser", "password123", RoleName.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Role not found: ADMIN");
        verify(userRepository).existsByUsername("newuser");
        verify(roleRepository).findByName(RoleName.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен успешно обновить пользователя")
    void updateUser_ShouldUpdateUser_WhenValidData() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("newusername");
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении с существующим именем пользователя")
    void updateUser_ShouldThrowException_WhenUsernameExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existingusername")).thenReturn(true);
        updateRequest.setUsername("existingusername");

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Username already exists: existingusername");
        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("existingusername");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен успешно удалить пользователя без карт с балансом")
    void deleteUser_ShouldDeleteUser_WhenNoCardsWithBalance() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.hasUserCardsWithBalance(1L)).thenReturn(false);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).hasUserCardsWithBalance(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Должен выбросить исключение при удалении пользователя с картами с балансом")
    void deleteUser_ShouldThrowException_WhenUserHasCardsWithBalance() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.hasUserCardsWithBalance(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Cannot delete user with cards having non-zero balance");
        verify(userRepository).findById(1L);
        verify(userRepository).hasUserCardsWithBalance(1L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Должен вернуть страницу всех пользователей")
    void getAllUsers_ShouldReturnPageOfUsers() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<User> result = userService.getAllUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUsername()).isEqualTo("testuser");
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Должен вернуть пользователей по роли")
    void getUsersByRole_ShouldReturnUsersByRole() {
        // Given
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(testRole));
        when(userRepository.findByRolesContaining(testRole)).thenReturn(List.of(testUser));

        // When
        List<User> result = userService.getUsersByRole(RoleName.USER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUsername()).isEqualTo("testuser");
        verify(roleRepository).findByName(RoleName.USER);
        verify(userRepository).findByRolesContaining(testRole);
    }

    @Test
    @DisplayName("Должен вернуть true для администратора")
    void isAdmin_ShouldReturnTrue_WhenUserIsAdmin() {
        // Given
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        testUser.setRoles(Collections.singleton(adminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.isAdmin(1L);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Должен вернуть false для обычного пользователя")
    void isAdmin_ShouldReturnFalse_WhenUserIsNotAdmin() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.isAdmin(1L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при проверке роли несуществующего пользователя")
    void isAdmin_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.isAdmin(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 999");
        verify(userRepository).findById(999L);
    }
}
