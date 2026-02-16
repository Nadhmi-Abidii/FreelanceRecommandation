package com.towork.milestone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.towork.common.BaseEntity;
import com.towork.user.entity.Freelancer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * File or link uploaded by the freelancer for a milestone.
 */
@Entity
@Table(name = "milestone_deliverables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MilestoneDeliverable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    @JsonIgnore
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Freelancer uploader;

    /**
     * Original filename sent by the freelancer.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Key or path stored on disk (using FileStorageService).
     */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
