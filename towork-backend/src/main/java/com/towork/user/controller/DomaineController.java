package com.towork.user.controller;

import com.towork.ai.dto.AiDomainSuggestion;
import com.towork.ai.dto.AiDomainSuggestionRequest;
import com.towork.ai.service.AiFeatureService;
import com.towork.config.MessageResponse;
import com.towork.user.entity.Domaine;
import com.towork.user.service.DomaineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/domaines")
@RequiredArgsConstructor
public class DomaineController {

    private final DomaineService domaineService;
    private final AiFeatureService aiFeatureService;

    @PostMapping
    public ResponseEntity<MessageResponse> createDomaine(@RequestBody Domaine domaine) {
        Domaine createdDomaine = domaineService.createDomaine(domaine);
        return ResponseEntity.ok(MessageResponse.success("Domaine created successfully", createdDomaine));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getDomaineById(@PathVariable Long id) {
        Domaine domaine = domaineService.getDomaineById(id);
        return ResponseEntity.ok(MessageResponse.success("Domaine retrieved successfully", domaine));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllDomaines(Pageable pageable) {
        Page<Domaine> domaines = domaineService.getAllDomaines(pageable);
        return ResponseEntity.ok(MessageResponse.success("Domaines retrieved successfully", domaines));
    }

    @GetMapping("/active")
    public ResponseEntity<MessageResponse> getActiveDomaines() {
        List<Domaine> domaines = domaineService.getActiveDomaines();
        return ResponseEntity.ok(MessageResponse.success("Active domaines retrieved successfully", domaines));
    }

    @GetMapping("/search")
    public ResponseEntity<MessageResponse> searchDomaines(@RequestParam String keyword) {
        List<Domaine> domaines = domaineService.searchDomaines(keyword);
        return ResponseEntity.ok(MessageResponse.success("Search results retrieved successfully", domaines));
    }

    @PostMapping("/suggest")
    public ResponseEntity<MessageResponse> suggestDomaines(@RequestBody AiDomainSuggestionRequest request) {
        List<Domaine> domaines = domaineService.getActiveDomaines();
        List<AiDomainSuggestion> suggestions = aiFeatureService.suggestDomaines(request, domaines);
        return ResponseEntity.ok(MessageResponse.success("Domain suggestions generated", suggestions));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateDomaine(@PathVariable Long id, @RequestBody Domaine domaine) {
        Domaine updatedDomaine = domaineService.updateDomaine(id, domaine);
        return ResponseEntity.ok(MessageResponse.success("Domaine updated successfully", updatedDomaine));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<MessageResponse> activateDomaine(@PathVariable Long id) {
        Domaine domaine = domaineService.activateDomaine(id);
        return ResponseEntity.ok(MessageResponse.success("Domaine activated successfully", domaine));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> deactivateDomaine(@PathVariable Long id) {
        Domaine domaine = domaineService.deactivateDomaine(id);
        return ResponseEntity.ok(MessageResponse.success("Domaine deactivated successfully", domaine));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteDomaine(@PathVariable Long id) {
        domaineService.deleteDomaine(id);
        return ResponseEntity.ok(MessageResponse.success("Domaine deleted successfully"));
    }
}
