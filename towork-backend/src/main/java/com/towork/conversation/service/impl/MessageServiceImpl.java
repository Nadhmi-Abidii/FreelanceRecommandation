package com.towork.conversation.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.ai.dto.AiModerationResponse;
import com.towork.ai.service.AiModerationService;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.towork.conversation.entity.Message;
import com.towork.conversation.repository.MessageRepository;
import com.towork.conversation.service.MessageService;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final AiModerationService aiModerationService;

    @Override
    public Message createMessage(Message message) {
        // Business Logic: Validate sender and receiver exist
        Client sender = clientRepository.findById(message.getSender().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        Freelancer receiver = freelancerRepository.findById(message.getReceiver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        // Business Logic: Validate message content is not empty
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new BusinessException("Message content cannot be empty");
        }

        AiModerationResponse moderation = aiModerationService.moderate(message.getContent());
        applyModeration(message, moderation);
        if (aiModerationService.shouldBlock(moderation)) {
            throw new BusinessException("Message content flagged as unsafe");
        }

        // Business Logic: Set initial status
        message.setIsRead(false);
        
        return messageRepository.save(message);
    }

    @Override
    public Message updateMessage(Long id, Message message) {
        Message existingMessage = getMessageById(id);
        
        // Business Logic: Only allow updates if message is not read
        if (existingMessage.getIsRead()) {
            throw new BusinessException("Cannot update a read message");
        }

        existingMessage.setSubject(message.getSubject());
        existingMessage.setContent(message.getContent());
        existingMessage.setMessageType(message.getMessageType());

        AiModerationResponse moderation = aiModerationService.moderate(existingMessage.getContent());
        applyModeration(existingMessage, moderation);
        if (aiModerationService.shouldBlock(moderation)) {
            throw new BusinessException("Message content flagged as unsafe");
        }
        
        return messageRepository.save(existingMessage);
    }

    @Override
    public void deleteMessage(Long id) {
        Message message = getMessageById(id);
        
        // Business Logic: Soft delete - mark as inactive
        message.setIsActive(false);
        messageRepository.save(message);
    }

    @Override
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }

    @Override
    public List<Message> getMessagesBySender(Long senderId) {
        Client sender = clientRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId));
        return messageRepository.findBySender(sender);
    }

    @Override
    public List<Message> getMessagesByReceiver(Long receiverId) {
        Freelancer receiver = freelancerRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + receiverId));
        return messageRepository.findByReceiver(receiver);
    }

    @Override
    public List<Message> getConversation(Long senderId, Long receiverId) {
        Client sender = clientRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId));
        
        Freelancer receiver = freelancerRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + receiverId));
        
        return messageRepository.findBySenderAndReceiver(sender, receiver);
    }

    @Override
    public Page<Message> getAllMessages(Pageable pageable) {
        return messageRepository.findAll(pageable);
    }

    @Override
    public Message markAsRead(Long id) {
        Message message = getMessageById(id);
        
        // Business Logic: Only mark as read if not already read
        if (message.getIsRead()) {
            throw new BusinessException("Message is already marked as read");
        }

        message.setIsRead(true);
        return messageRepository.save(message);
    }

    @Override
    public List<Message> getUnreadMessages(Long receiverId) {
        Freelancer receiver = freelancerRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + receiverId));
        return messageRepository.findUnreadMessagesByReceiver(receiver);
    }

    @Override
    public Long countUnreadMessages(Long receiverId) {
        Freelancer receiver = freelancerRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + receiverId));
        return messageRepository.countUnreadMessagesByReceiver(receiver);
    }

    private void applyModeration(Message message, AiModerationResponse moderation) {
        if (moderation == null) {
            return;
        }
        message.setIsFlagged(Boolean.TRUE.equals(moderation.getFlagged()));
        message.setFlagScore(moderation.getScore());
        message.setFlagLabel(moderation.getLabel());
        message.setFlagReason(moderation.getReason());
    }
}

