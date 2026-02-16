package com.towork.candidature.controller;

import com.towork.ai.dto.AiSummaryResponse;
import com.towork.ai.service.AiFeatureService;
import com.towork.config.MessageResponse;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.candidature.dto.CandidatureMessageResponse;
import com.towork.candidature.dto.CandidatureResponse;
import com.towork.candidature.dto.CreateCandidatureMessageRequest;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;
import com.towork.candidature.entity.CandidatureMessageAuthor;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.candidature.mapper.CandidatureMapper;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.candidature.service.CandidatureService;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final CandidatureService candidatureService;
    private final CandidatureRepository candidatureRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final AiFeatureService aiFeatureService;

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getMyConversations(Authentication authentication) {
        boolean isFreelancer = hasAuthority(authentication, "ROLE_FREELANCER");
        List<CandidatureResponse> conversations;

        if (isFreelancer) {
            Freelancer freelancer = freelancerRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
            conversations = candidatureRepository.findByFreelancerAndStatus(freelancer, CandidatureStatus.ACCEPTED)
                    .stream()
                    .map(CandidatureMapper::toDto)
                    .toList();
        } else {
            Client client = clientRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            conversations = candidatureRepository.findByMissionClientAndStatus(client, CandidatureStatus.ACCEPTED)
                    .stream()
                    .map(CandidatureMapper::toDto)
                    .toList();
        }

        return ResponseEntity.ok(MessageResponse.success("Conversations retrieved successfully", conversations));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getConversationMessages(@PathVariable Long id, Authentication authentication) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        ensureConversationAccess(candidature, authentication);
        if (candidature.getStatus() != CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Messaging is available once the candidature is accepted");
        }
        List<CandidatureMessageResponse> messages = candidatureService.getMessages(id).stream()
                .map(CandidatureMapper::toMessageDto)
                .toList();
        return ResponseEntity.ok(MessageResponse.success("Conversation messages retrieved successfully", messages));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getConversationSummary(@PathVariable Long id,
                                                                  @RequestParam(required = false) String language,
                                                                  Authentication authentication) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        ensureConversationAccess(candidature, authentication);
        List<CandidatureMessage> messages = candidatureService.getMessages(id);
        int maxMessages = 50;
        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }
        List<String> content = messages.stream()
                .map(CandidatureMessage::getContent)
                .toList();
        String context = candidature.getMission() != null ? candidature.getMission().getTitle() : null;
        AiSummaryResponse summary = aiFeatureService.summarizeConversation(content, context, language);
        return ResponseEntity.ok(MessageResponse.success("Conversation summary generated", summary));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> postConversationMessage(@PathVariable Long id,
                                                                    @RequestBody CreateCandidatureMessageRequest request,
                                                                    Authentication authentication) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        CandidatureMessageAuthor author = resolveAuthor(candidature, authentication);
        if (candidature.getStatus() != CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Messaging is available once the candidature is accepted");
        }
        CandidatureMessage message = candidatureService.addMessage(id, author, request.getContent(), request.getResumeUrl());
        return ResponseEntity.ok(MessageResponse.success("Conversation message created successfully", CandidatureMapper.toMessageDto(message)));
    }

    private void ensureConversationAccess(Candidature candidature, Authentication authentication) {
        resolveAuthor(candidature, authentication);
    }

    private CandidatureMessageAuthor resolveAuthor(Candidature candidature, Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException("Authentication is required to access this conversation");
        }
        boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");
        String email = authentication.getName();
        boolean isClient = candidature.getMission() != null
                && candidature.getMission().getClient() != null
                && email.equalsIgnoreCase(candidature.getMission().getClient().getEmail());
        boolean isFreelancer = candidature.getFreelancer() != null
                && email.equalsIgnoreCase(candidature.getFreelancer().getEmail());

        if (isClient) {
            return CandidatureMessageAuthor.CLIENT;
        }
        if (isFreelancer) {
            return CandidatureMessageAuthor.FREELANCER;
        }
        if (isAdmin) {
            return CandidatureMessageAuthor.CLIENT;
        }
        throw new BusinessException("You cannot access this conversation");
    }

    private boolean hasAuthority(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
