package com.towork.contract.controller;

import com.towork.config.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.contract.entity.Contrat;
import com.towork.contract.entity.EtatContrat;
import com.towork.contract.service.ContratService;

@RestController
@RequestMapping("/contrats")
@RequiredArgsConstructor
public class ContratController {

    private final ContratService contratService;

    @PostMapping
    public ResponseEntity<MessageResponse> createContrat(@RequestBody Contrat contrat) {
        Contrat createdContrat = contratService.createContrat(contrat);
        return ResponseEntity.ok(MessageResponse.success("Contrat created successfully", createdContrat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getContratById(@PathVariable Long id) {
        Contrat contrat = contratService.getContratById(id);
        return ResponseEntity.ok(MessageResponse.success("Contrat retrieved successfully", contrat));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllContrats(Pageable pageable) {
        Page<Contrat> contrats = contratService.getAllContrats(pageable);
        return ResponseEntity.ok(MessageResponse.success("Contrats retrieved successfully", contrats));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<MessageResponse> getContratsByClient(@PathVariable Long clientId) {
        List<Contrat> contrats = contratService.getContratsByClient(clientId);
        return ResponseEntity.ok(MessageResponse.success("Contrats retrieved successfully", contrats));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<MessageResponse> getContratsByFreelancer(@PathVariable Long freelancerId) {
        List<Contrat> contrats = contratService.getContratsByFreelancer(freelancerId);
        return ResponseEntity.ok(MessageResponse.success("Contrats retrieved successfully", contrats));
    }

    @GetMapping("/etat/{etat}")
    public ResponseEntity<MessageResponse> getContratsByEtat(@PathVariable EtatContrat etat) {
        List<Contrat> contrats = contratService.getContratsByEtat(etat);
        return ResponseEntity.ok(MessageResponse.success("Contrats retrieved successfully", contrats));
    }

    @PutMapping("/{id}/etat")
    public ResponseEntity<MessageResponse> updateContratEtat(@PathVariable Long id, @RequestParam EtatContrat etat) {
        Contrat contrat = contratService.updateContratEtat(id, etat);
        return ResponseEntity.ok(MessageResponse.success("Contrat etat updated successfully", contrat));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteContrat(@PathVariable Long id) {
        contratService.deleteContrat(id);
        return ResponseEntity.ok(MessageResponse.success("Contrat deleted successfully"));
    }
}
