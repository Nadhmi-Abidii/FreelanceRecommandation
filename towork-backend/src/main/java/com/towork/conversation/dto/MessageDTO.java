package com.towork.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String receiverName;
    private String subject;
    private String content;
    private Boolean isRead;
    private String messageType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
