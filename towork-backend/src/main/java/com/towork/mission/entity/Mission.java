package com.towork.mission.entity;

import com.towork.common.BaseEntity;
import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;
import com.towork.user.entity.Freelancer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Mission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domaine_id", nullable = false)
    private Domaine domaine;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_freelancer_id")
    private Freelancer assignedFreelancer;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false)
    private BudgetType budgetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_travail", nullable = false)
    private TypeTravail typeTravail;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_experience", nullable = false)
    private NiveauExperience niveauExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MissionStatus status = MissionStatus.PUBLISHED;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration; // in days

    @Column(name = "skills_required", columnDefinition = "TEXT")
    private String skillsRequired;

    @Column(name = "is_urgent", nullable = false)
    private Boolean isUrgent = false;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // JSON string of file paths
}
