package com.towork.conversation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import com.towork.conversation.entity.Message;

public interface MessageService {
    Message createMessage(Message message);
    Message updateMessage(Long id, Message message);
    void deleteMessage(Long id);
    Message getMessageById(Long id);
    List<Message> getMessagesBySender(Long senderId);
    List<Message> getMessagesByReceiver(Long receiverId);
    List<Message> getConversation(Long senderId, Long receiverId);
    Page<Message> getAllMessages(Pageable pageable);
    Message markAsRead(Long id);
    List<Message> getUnreadMessages(Long receiverId);
    Long countUnreadMessages(Long receiverId);
}
