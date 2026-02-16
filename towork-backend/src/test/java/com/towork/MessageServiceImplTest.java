package com.towork;


import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.conversation.entity.Message;
import com.towork.conversation.repository.MessageRepository;
import com.towork.conversation.service.impl.MessageServiceImpl;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private com.towork.user.repository.ClientRepository clientRepository;
    @Mock private com.towork.user.repository.FreelancerRepository freelancerRepository;

    @InjectMocks
    private MessageServiceImpl service;

    private Client sender;
    private Freelancer receiver;

    @BeforeEach
    void setUp() {
        sender = new Client();
        sender.setId(1L);

        receiver = new Freelancer();
        receiver.setId(2L);
    }

    private Message newMessage() {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setSubject("Hi");
        message.setContent("Bonjour");
        message.setIsActive(true);
        return message;
    }

    @Test
    @DisplayName("createMessage valide les identités et le contenu")
    void create_ok() {
        Message message = newMessage();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(freelancerRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message saved = service.createMessage(message);

        assertThat(saved.getIsRead()).isFalse();
        verify(messageRepository).save(saved);
    }

    @Test
    @DisplayName("createMessage échoue si contenu vide ou identité absente")
    void create_guardrails() {
        Message message = newMessage();
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMessage(message))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sender not found");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(freelancerRepository.findById(2L)).thenReturn(Optional.of(receiver));
        message.setContent("   ");

        assertThatThrownBy(() -> service.createMessage(message))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("content cannot be empty");
    }

    @Test
    @DisplayName("updateMessage autorise seulement si non lu")
    void update_guardrails() {
        Message existing = newMessage();
        existing.setIsRead(true);
        when(messageRepository.findById(3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateMessage(3L, newMessage()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot update a read message");
    }

    @Test
    @DisplayName("markAsRead bascule le flag ou lève une exception si déjà lu")
    void markAsRead_behavior() {
        Message unread = newMessage();
        unread.setIsRead(false);
        when(messageRepository.findById(4L)).thenReturn(Optional.of(unread));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        Message read = service.markAsRead(4L);
        assertThat(read.getIsRead()).isTrue();

        when(messageRepository.findById(5L)).thenReturn(Optional.of(read));
        assertThatThrownBy(() -> service.markAsRead(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already marked as read");
    }

    @Test
    @DisplayName("getters et conversations délèguent aux repositories")
    void repositoryDelegations() {
        Message message = newMessage();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(freelancerRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.findBySender(sender)).thenReturn(List.of(message));
        when(messageRepository.findByReceiver(receiver)).thenReturn(List.of(message));
        when(messageRepository.findBySenderAndReceiver(sender, receiver)).thenReturn(List.of(message));
        when(messageRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(message)));
        when(messageRepository.findUnreadMessagesByReceiver(receiver)).thenReturn(List.of(message));
        when(messageRepository.countUnreadMessagesByReceiver(receiver)).thenReturn(1L);

        assertThat(service.getMessagesBySender(1L)).hasSize(1);
        assertThat(service.getMessagesByReceiver(2L)).hasSize(1);
        assertThat(service.getConversation(1L, 2L)).hasSize(1);
        assertThat(service.getAllMessages(Pageable.unpaged()).getContent()).hasSize(1);
        assertThat(service.getUnreadMessages(2L)).hasSize(1);
        assertThat(service.countUnreadMessages(2L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("getMessageById lève ResourceNotFoundException si absent")
    void getMessageById_missing() {
        when(messageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessageById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }
}