package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.UserDTOResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.RoleName;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Secured("ADMIN")
public class UserManagementController {

    private final UserService userService;

    private final UserMapper userMapper;

    // Получить всех пользователей (с пагинацией)
    @GetMapping
    public ResponseEntity<Page<UserDTOResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userService.getAllUsers(pageable);
        Page<UserDTOResponse> userDTOs = users.map(userMapper::userToUserDTOResponse);
        return ResponseEntity.ok(userDTOs);
    }

    // Получить пользователя по ID
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTOResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserDTOResponse userDTO = userMapper.userToUserDTOResponse(user);
        return ResponseEntity.ok(userDTO);
    }

    // Создать нового пользователя
    @PostMapping
    public ResponseEntity<UserDTOResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRole() != null ? request.getRole() : RoleName.USER
        );
        UserDTOResponse userDTO = userMapper.userToUserDTOResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }

    // Обновить пользователя
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTOResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(userId, request);
        UserDTOResponse userDTO = userMapper.userToUserDTOResponse(user);
        return ResponseEntity.ok(userDTO);
    }

    // Удалить пользователя
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // Получить пользователей по роли
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTOResponse>> getUsersByRole(@PathVariable RoleName role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserDTOResponse> userDTOs = users.stream()
                .map(userMapper::userToUserDTOResponse)
                .toList();
        return ResponseEntity.ok(userDTOs);
    }
}
