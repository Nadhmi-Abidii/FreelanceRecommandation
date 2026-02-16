package com.towork.conversation.repository;

import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.towork.conversation.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySender(Client sender);

    List<Message> findByReceiver(Freelancer receiver);

    List<Message> findBySenderAndReceiver(Client sender, Freelancer receiver);

    @Query("SELECT m FROM Message m WHERE m.receiver = :receiver AND m.isRead = false")
    List<Message> findUnreadMessagesByReceiver(@Param("receiver") Freelancer receiver);

    @Query("SELECT m FROM Message m WHERE m.sender = :sender AND m.isRead = false")
    List<Message> findUnreadMessagesBySender(@Param("sender") Client sender);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :receiver AND m.isRead = false")
    Long countUnreadMessagesByReceiver(@Param("receiver") Freelancer receiver);
}
