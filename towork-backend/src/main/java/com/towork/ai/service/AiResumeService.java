package com.towork.ai.service;

import com.towork.ai.config.AiProperties;
import com.towork.ai.dto.AiResumeExtractionResponse;
import com.towork.ai.dto.AiResumeExtractionResult;
import com.towork.ai.dto.AiSkillDto;
import com.towork.exception.ResourceNotFoundException;
import com.towork.file.FileStorageService;
import com.towork.user.entity.Competence;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.CompetenceRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiResumeService {

    private final FileStorageService fileStorageService;
    private final AiFeatureService aiFeatureService;
    private final AiProperties properties;
    private final FreelancerRepository freelancerRepository;
    private final CompetenceRepository competenceRepository;

    public AiResumeExtractionResponse extractSkills(MultipartFile file, Long freelancerId, String language) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        try {
            String fileKey = fileStorageService.storeFile(file);
            String resumeText = extractPdfText(fileKey);
            resumeText = trimToLength(resumeText, properties.getOpenai().getMaxInputChars());

            AiResumeExtractionResult result = aiFeatureService.extractSkillsFromResume(resumeText, language);
            int createdCount = 0;
            if (freelancerId != null) {
                createdCount = createCompetences(freelancerId, result.getSkills());
            }
            return new AiResumeExtractionResponse(fileKey, result.getSummary(), result.getSkills(), createdCount);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to process resume file", ex);
        }
    }

    private String extractPdfText(String fileKey) throws IOException {
        byte[] bytes = fileStorageService.loadFileAsBytes(fileKey);
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private int createCompetences(Long freelancerId, List<AiSkillDto> skills) {
        if (skills == null || skills.isEmpty()) {
            return 0;
        }
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
        int created = 0;
        for (AiSkillDto skill : skills) {
            if (skill == null || skill.getName() == null || skill.getName().isBlank()) {
                continue;
            }
            String name = skill.getName().trim();
            if (!competenceRepository.findByNameAndFreelancer(name, freelancer).isEmpty()) {
                continue;
            }
            Competence competence = new Competence();
            competence.setFreelancer(freelancer);
            competence.setName(name);
            competence.setLevel(normalizeLevel(skill.getLevel()));
            competence.setYearsOfExperience(skill.getYearsOfExperience());
            competence.setIsCertified(Boolean.TRUE.equals(skill.getIsCertified()));
            competence.setCertificationName(skill.getCertificationName());
            competenceRepository.save(competence);
            created++;
        }
        return created;
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "BEGINNER";
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT" -> normalized;
            default -> "BEGINNER";
        };
    }

    private String trimToLength(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength));
    }
}
