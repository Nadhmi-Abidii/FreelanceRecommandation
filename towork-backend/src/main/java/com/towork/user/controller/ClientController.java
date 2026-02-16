package com.towork.user.controller;

import com.towork.config.MessageResponse;
import com.towork.user.entity.Client;
import com.towork.user.dto.ClientDto;
import com.towork.user.dto.UpdateClientProfileRequest;
import com.towork.user.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /* ===================== UTILISATEUR CONNECTÉ ===================== */

    // GET /clients/me => profil de l'utilisateur connecté (email depuis le JWT)
    @GetMapping("/me")
    public ResponseEntity<MessageResponse> getMe() {
        String email = currentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Non authentifié"));
        }
        Client me = clientService.getClientByEmail(email);
        ClientDto dto = clientService.convertToDto(me);
        return ResponseEntity.ok(MessageResponse.success("Client profile", dto));
    }

    // PUT /clients/me/profile => mise à jour de MES infos (sans toucher à email/password ici)
    @PutMapping("/me/profile")
    public ResponseEntity<MessageResponse> updateMyProfile(@RequestBody UpdateClientProfileRequest req) {
        String email = currentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Non authentifié"));
        }
        Client me = clientService.getClientByEmail(email);

        // Mapper UNIQUEMENT les champs fournis (les null/vides seront ignorés dans le service)
        Client incoming = new Client();
        incoming.setFirstName(req.getFirstName());
        incoming.setLastName(req.getLastName());
        incoming.setPhone(req.getPhone());
        incoming.setCompanyName(req.getCompanyName());
        incoming.setCompanySize(req.getCompanySize());
        incoming.setIndustry(req.getIndustry());
        incoming.setWebsite(req.getWebsite());
        incoming.setAddress(req.getAddress());
        incoming.setCity(req.getCity());
        incoming.setCountry(req.getCountry());
        incoming.setPostalCode(req.getPostalCode());
        incoming.setProfilePicture(req.getProfilePicture());
        incoming.setBio(req.getBio());

        Client updated = clientService.updateClientProfile(me.getId(), incoming);
        ClientDto dto = clientService.convertToDto(updated);
        return ResponseEntity.ok(MessageResponse.success("Client profile updated successfully", dto));
    }

    /* ===================== ROUTES GÉNÉRIQUES (ADMIN / PUBLIC) ===================== */

    @PostMapping
    public ResponseEntity<MessageResponse> createClient(@RequestBody Client client) {
        Client createdClient = clientService.createClient(client);
        return ResponseEntity.ok(MessageResponse.success("Client created ", createdClient));
    }

    // IMPORTANT: restreint aux chiffres pour ne pas attraper "/me"
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MessageResponse> getClientById(@PathVariable Long id) {
        Client client = clientService.getClientById(id);
        return ResponseEntity.ok(MessageResponse.success("Client retrieved ", client));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllClients(Pageable pageable) {
        Page<Client> clients = clientService.getAllClients(pageable);
        return ResponseEntity.ok(MessageResponse.success("Clients retrieved ", clients));
    }

    @GetMapping("/verified")
    public ResponseEntity<MessageResponse> getVerifiedClients() {
        List<Client> clients = clientService.getVerifiedClients();
        return ResponseEntity.ok(MessageResponse.success("Verified clients retrieved successfully", clients));
    }

    @GetMapping("/search")
    public ResponseEntity<MessageResponse> searchClients(@RequestParam String keyword) {
        List<Client> clients = clientService.searchClients(keyword);
        return ResponseEntity.ok(MessageResponse.success("Search results retrieved successfully", clients));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MessageResponse> updateClient(@PathVariable Long id, @RequestBody Client client) {
        Client updatedClient = clientService.updateClient(id, client);
        return ResponseEntity.ok(MessageResponse.success("Client updated successfully", updatedClient));
    }

    @PutMapping("/{id:\\d+}/profile")
    public ResponseEntity<MessageResponse> updateClientProfile(@PathVariable Long id, @RequestBody Client client) {
        Client updatedClient = clientService.updateClientProfile(id, client);
        return ResponseEntity.ok(MessageResponse.success("Client profile updated successfully", updatedClient));
    }

    @PutMapping("/{id:\\d+}/verify")
    public ResponseEntity<MessageResponse> verifyClient(@PathVariable Long id) {
        Client client = clientService.verifyClient(id);
        return ResponseEntity.ok(MessageResponse.success("Client verified successfully", client));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<MessageResponse> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok(MessageResponse.success("Client deleted successfully"));
    }

    /* ===================== UTIL ===================== */

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null; // getName() = subject/email du JWT
    }
}
