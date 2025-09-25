package com.example.bankcards.dto.response;

import com.example.bankcards.enums.RoleName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDTOResponse {
    private Long id;
    private String username;
    private Set<RoleName> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
