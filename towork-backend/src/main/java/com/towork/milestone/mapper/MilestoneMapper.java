package com.towork.milestone.mapper;

import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.towork.milestone.dto.MilestoneDeliverableDto;
import com.towork.milestone.dto.MilestoneDto;

public final class MilestoneMapper {

    private MilestoneMapper() {
    }

    public static MilestoneDto toDto(Milestone milestone, List<MilestoneDeliverable> deliverables, String downloadBasePath) {
        if (milestone == null) {
            return null;
        }
        List<MilestoneDeliverable> safeDeliverables = deliverables != null ? deliverables : Collections.emptyList();
        String base = (downloadBasePath == null || downloadBasePath.isBlank())
                ? "/api/milestones/deliverables"
                : downloadBasePath;

        List<MilestoneDeliverableDto> deliverableDtos = safeDeliverables.stream()
                .map(d -> toDeliverableDto(d, base))
                .toList();

        return new MilestoneDto(
                milestone.getId(),
                milestone.getMission() != null ? milestone.getMission().getId() : null,
                milestone.getMission() != null ? milestone.getMission().getTitle() : null,
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getAmount(),
                milestone.getDueDate(),
                milestone.getStatus(),
                milestone.getIsCompleted(),
                milestone.getCompletionDate(),
                milestone.getCompletionNotes(),
                milestone.getRejectionReason(),
                milestone.getPaidAt(),
                milestone.getDeliverableFileName(),
                milestone.getDeliverableOriginalName(),
                milestone.getDeliverableFileType(),
                milestone.getDeliverableFileSize(),
                milestone.getDeliverableUploadedAt(),
                milestone.getDeliverableComment(),
                milestone.getOrderIndex(),
                deliverableDtos
        );
    }

    public static MilestoneDeliverableDto toDeliverableDto(MilestoneDeliverable deliverable, String downloadBasePath) {
        if (deliverable == null) {
            return null;
        }
        String base = (downloadBasePath == null || downloadBasePath.isBlank())
                ? "/api/milestones/deliverables"
                : downloadBasePath;
        String url = base.endsWith("/")
                ? base + deliverable.getId() + "/download"
                : base + "/" + deliverable.getId() + "/download";
        String uploadedBy = Optional.ofNullable(deliverable.getUploader())
                .map(u -> {
                    String fullName = String.join(" ",
                            u.getFirstName() != null ? u.getFirstName() : "",
                            u.getLastName() != null ? u.getLastName() : "").trim();
                    return fullName.isBlank() ? u.getEmail() : fullName;
                })
                .orElse(null);

        return new MilestoneDeliverableDto(
                deliverable.getId(),
                deliverable.getFileName(),
                url,
                deliverable.getComment(),
                deliverable.getContentType(),
                uploadedBy,
                deliverable.getCreatedAt()
        );
    }
}
