package com.example.bankcards.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String error;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private List<String> details;
    
    public static ErrorResponse of(String message, String error, int status, String path) {
        return ErrorResponse.builder()
                .message(message)
                .error(error)
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
