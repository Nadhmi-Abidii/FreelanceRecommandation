package com.towork.candidature.dto;

import lombok.Data;
import com.towork.candidature.entity.CandidatureMessageAuthor;

@Data
public class CreateCandidatureMessageRequest {
    private CandidatureMessageAuthor author;
    private String content;
    private String resumeUrl;
}
