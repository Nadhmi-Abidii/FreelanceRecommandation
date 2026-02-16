package com.towork.candidature.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import com.towork.candidature.entity.CandidatureMessageAuthor;

@Value
@Builder
public class CandidatureMessageResponse {
    Long id;
    CandidatureMessageAuthor author;
    String content;
    String resumeUrl;
    Boolean isFlagged;
    Double flagScore;
    String flagLabel;
    String flagReason;
    LocalDateTime createdAt;
}
