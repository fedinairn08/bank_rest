package com.example.bankcards.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String roles;

    public AuthResponse(String accessToken, String username, String roles) {
        this.accessToken = accessToken;
        this.username = username;
        this.roles = roles;
    }
}