package com.towork.user.controller;

import com.towork.config.MessageResponse;
import com.towork.user.entity.Competence;
import com.towork.user.service.CompetenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/competences")
@RequiredArgsConstructor
public class CompetenceController {

    private final CompetenceService competenceService;

    @PostMapping
    public ResponseEntity<MessageResponse> createCompetence(@RequestBody Competence competence) {
        Competence createdCompetence = competenceService.createCompetence(competence);
        return ResponseEntity.ok(MessageResponse.success("Competence created successfully", createdCompetence));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getCompetenceById(@PathVariable Long id) {
        Competence competence = competenceService.getCompetenceById(id);
        return ResponseEntity.ok(MessageResponse.success("Competence retrieved successfully", competence));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllCompetences(Pageable pageable) {
        Page<Competence> competences = competenceService.getAllCompetences(pageable);
        return ResponseEntity.ok(MessageResponse.success("Competences retrieved successfully", competences));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<MessageResponse> getCompetencesByFreelancer(@PathVariable Long freelancerId) {
        List<Competence> competences = competenceService.getCompetencesByFreelancer(freelancerId);
        return ResponseEntity.ok(MessageResponse.success("Competences retrieved successfully", competences));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<MessageResponse> getCompetencesByName(@PathVariable String name) {
        List<Competence> competences = competenceService.getCompetencesByName(name);
        return ResponseEntity.ok(MessageResponse.success("Competences retrieved successfully", competences));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<MessageResponse> getCompetencesByLevel(@PathVariable String level) {
        List<Competence> competences = competenceService.getCompetencesByLevel(level);
        return ResponseEntity.ok(MessageResponse.success("Competences retrieved successfully", competences));
    }

    @GetMapping("/certified")
    public ResponseEntity<MessageResponse> getCertifiedCompetences() {
        List<Competence> competences = competenceService.getCertifiedCompetences();
        return ResponseEntity.ok(MessageResponse.success("Certified competences retrieved successfully", competences));
    }

    @GetMapping("/names")
    public ResponseEntity<MessageResponse> getDistinctCompetenceNames() {
        List<String> names = competenceService.getDistinctCompetenceNames();
        return ResponseEntity.ok(MessageResponse.success("Competence names retrieved successfully", names));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateCompetence(@PathVariable Long id, @RequestBody Competence competence) {
        Competence updatedCompetence = competenceService.updateCompetence(id, competence);
        return ResponseEntity.ok(MessageResponse.success("Competence updated successfully", updatedCompetence));
    }

    @PutMapping("/{id}/certify")
    public ResponseEntity<MessageResponse> certifyCompetence(@PathVariable Long id, @RequestParam String certificationName) {
        Competence competence = competenceService.certifyCompetence(id, certificationName);
        return ResponseEntity.ok(MessageResponse.success("Competence certified successfully", competence));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCompetence(@PathVariable Long id) {
        competenceService.deleteCompetence(id);
        return ResponseEntity.ok(MessageResponse.success("Competence deleted successfully"));
    }
}
