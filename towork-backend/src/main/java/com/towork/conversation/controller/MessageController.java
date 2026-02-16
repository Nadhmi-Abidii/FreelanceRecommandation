package com.towork.conversation.controller;

import com.towork.config.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.conversation.entity.Message;
import com.towork.conversation.service.MessageService;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(@RequestBody Message message) {
        Message createdMessage = messageService.createMessage(message);
        return ResponseEntity.ok(MessageResponse.success("Message created successfully", createdMessage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable Long id) {
        Message message = messageService.getMessageById(id);
        return ResponseEntity.ok(MessageResponse.success("Message retrieved successfully", message));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllMessages(Pageable pageable) {
        Page<Message> messages = messageService.getAllMessages(pageable);
        return ResponseEntity.ok(MessageResponse.success("Messages retrieved successfully", messages));
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<MessageResponse> getMessagesBySender(@PathVariable Long senderId) {
        List<Message> messages = messageService.getMessagesBySender(senderId);
        return ResponseEntity.ok(MessageResponse.success("Messages retrieved successfully", messages));
    }

    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<MessageResponse> getMessagesByReceiver(@PathVariable Long receiverId) {
        List<Message> messages = messageService.getMessagesByReceiver(receiverId);
        return ResponseEntity.ok(MessageResponse.success("Messages retrieved successfully", messages));
    }

    @GetMapping("/conversation")
    public ResponseEntity<MessageResponse> getConversation(
            @RequestParam Long senderId,
            @RequestParam Long receiverId) {
        List<Message> messages = messageService.getConversation(senderId, receiverId);
        return ResponseEntity.ok(MessageResponse.success("Conversation retrieved successfully", messages));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable Long id) {
        Message message = messageService.markAsRead(id);
        return ResponseEntity.ok(MessageResponse.success("Message marked as read", message));
    }

    @GetMapping("/unread/{receiverId}")
    public ResponseEntity<MessageResponse> getUnreadMessages(@PathVariable Long receiverId) {
        List<Message> messages = messageService.getUnreadMessages(receiverId);
        return ResponseEntity.ok(MessageResponse.success("Unread messages retrieved successfully", messages));
    }

    @GetMapping("/unread/count/{receiverId}")
    public ResponseEntity<MessageResponse> countUnreadMessages(@PathVariable Long receiverId) {
        Long count = messageService.countUnreadMessages(receiverId);
        return ResponseEntity.ok(MessageResponse.success("Unread messages count retrieved successfully", count));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok(MessageResponse.success("Message deleted successfully"));
    }
}
