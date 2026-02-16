package com.towork.ai.controller;

import com.towork.ai.dto.AiDraftRequest;
import com.towork.ai.dto.AiDraftResponse;
import com.towork.ai.dto.AiModerationRequest;
import com.towork.ai.dto.AiModerationResponse;
import com.towork.ai.dto.AiResumeExtractionResponse;
import com.towork.ai.dto.AiRewriteRequest;
import com.towork.ai.dto.AiRewriteResponse;
import com.towork.ai.service.AiFeatureService;
import com.towork.ai.service.AiModerationService;
import com.towork.ai.service.AiResumeService;
import com.towork.config.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFeatureService aiFeatureService;
    private final AiModerationService aiModerationService;
    private final AiResumeService aiResumeService;

    @PostMapping("/draft/mission")
    public ResponseEntity<MessageResponse> draftMission(@RequestBody AiDraftRequest request) {
        AiDraftResponse response = aiFeatureService.draftMission(request);
        return ResponseEntity.ok(MessageResponse.success("Mission draft generated", response));
    }

    @PostMapping("/rewrite")
    public ResponseEntity<MessageResponse> rewriteText(@RequestBody AiRewriteRequest request) {
        AiRewriteResponse response = aiFeatureService.rewriteText(request);
        return ResponseEntity.ok(MessageResponse.success("Rewrite completed", response));
    }

    @PostMapping("/moderate")
    public ResponseEntity<MessageResponse> moderate(@RequestBody AiModerationRequest request) {
        AiModerationResponse response = aiModerationService.moderate(request.getContent());
        return ResponseEntity.ok(MessageResponse.success("Moderation completed", response));
    }

    @PostMapping(value = "/resume/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> extractResume(@RequestPart("file") MultipartFile file,
                                                         @RequestParam(required = false) Long freelancerId,
                                                         @RequestParam(required = false) String language) {
        AiResumeExtractionResponse response = aiResumeService.extractSkills(file, freelancerId, language);
        return ResponseEntity.ok(MessageResponse.success("Resume processed", response));
    }
}
