package com.towork.conversation.entity;

import com.towork.common.BaseEntity;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Client sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Freelancer receiver;

    @Column(name = "subject")
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "message_type")
    private String messageType; // PROPOSAL, QUESTION, CLARIFICATION, etc.

    @Column(name = "is_flagged", nullable = false)
    private Boolean isFlagged = false;

    @Column(name = "flag_score")
    private Double flagScore;

    @Column(name = "flag_label")
    private String flagLabel;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;
}
