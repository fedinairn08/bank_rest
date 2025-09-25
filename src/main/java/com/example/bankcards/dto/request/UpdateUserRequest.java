package com.example.bankcards.dto.request;

import com.example.bankcards.enums.RoleName;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String password;
    private RoleName role;
}
