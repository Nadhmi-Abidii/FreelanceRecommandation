package com.towork.milestone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.towork.common.BaseEntity;
import com.towork.mission.entity.Mission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" })
public class Milestone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    @JsonIgnore // avoid lazy proxy serialization issues
    private Mission mission;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Status flow: DRAFT -> IN_PROGRESS -> SUBMITTED -> (REJECTED or COMPLETED -> PAID)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MilestoneStatus status = MilestoneStatus.DRAFT;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "deliverable_file_name")
    private String deliverableFileName;

    @Column(name = "deliverable_original_name")
    private String deliverableOriginalName;

    @Column(name = "deliverable_file_type")
    private String deliverableFileType;

    @Column(name = "deliverable_file_size")
    private Long deliverableFileSize;

    @Column(name = "deliverable_uploaded_at")
    private LocalDateTime deliverableUploadedAt;

    @Column(name = "deliverable_comment", columnDefinition = "TEXT")
    private String deliverableComment;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "milestone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<MilestoneDeliverable> deliverables;
}
