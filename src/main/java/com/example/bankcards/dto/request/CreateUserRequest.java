package com.example.bankcards.dto.request;

import com.example.bankcards.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private RoleName role = RoleName.USER;
}
