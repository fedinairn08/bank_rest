package com.example.bankcards.dto.request;

import com.example.bankcards.enums.RoleName;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    private RoleName role;
}
