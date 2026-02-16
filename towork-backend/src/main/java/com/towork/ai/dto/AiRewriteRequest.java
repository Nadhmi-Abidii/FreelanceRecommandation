package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRewriteRequest {
    private String content;
    private String intent;
    private String tone;
    private String language;
    private Integer maxLength;
}
