package com.towork.candidature.entity;

import com.towork.common.BaseEntity;
import com.towork.mission.entity.Mission;
import com.towork.user.entity.Freelancer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "messages")
@ToString(exclude = "messages")
public class Candidature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private Freelancer freelancer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "proposed_price")
    private Double proposedPrice;

    @Column(name = "proposed_duration")
    private Integer proposedDuration; // in days

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CandidatureStatus status = CandidatureStatus.PENDING;

    @Column(name = "client_message", columnDefinition = "TEXT")
    private String clientMessage;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CandidatureMessage> messages = new ArrayList<>();

    public void addMessage(CandidatureMessage message) {
        this.messages.add(message);
        message.setCandidature(this);
    }
}
