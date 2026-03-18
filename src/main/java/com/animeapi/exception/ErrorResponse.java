package com.animeapi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, LocalDateTime.now(), null);
    }

    public static ErrorResponse ofValidation(int status, String message, Map<String, String> errors) {
        return new ErrorResponse(status, message, LocalDateTime.now(), errors);
    }
}