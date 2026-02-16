package com.towork.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success;
    private Object data;

    public static MessageResponse success(String message) {
        return new MessageResponse(message, true, null);
    }

    public static MessageResponse success(String message, Object data) {
        return new MessageResponse(message, true, data);
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(message, false, null);
    }

    public static MessageResponse error(String message, Object data) {
        return new MessageResponse(message, false, data);
    }
}
