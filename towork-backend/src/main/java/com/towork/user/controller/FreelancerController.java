package com.towork.user.controller;

import com.towork.config.MessageResponse;
import com.towork.user.entity.Freelancer;
import com.towork.user.dto.FreelancerDto;
import com.towork.user.dto.UpdateFreelancerProfileRequest;
import com.towork.user.service.FreelancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/freelancers")
@RequiredArgsConstructor
public class FreelancerController {

    private final FreelancerService freelancerService;

    /* ===================== UTILISATEUR CONNECTE ===================== */

    @GetMapping("/me")
    public ResponseEntity<MessageResponse> getMe() {
        String email = currentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Non authentifie"));
        }
        Freelancer me = freelancerService.getFreelancerByEmail(email);
        FreelancerDto dto = freelancerService.convertToDto(me);
        return ResponseEntity.ok(MessageResponse.success("Freelancer profile", dto));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<MessageResponse> updateMyProfile(@RequestBody UpdateFreelancerProfileRequest req) {
        String email = currentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(MessageResponse.error("Non authentifie"));
        }
        Freelancer me = freelancerService.getFreelancerByEmail(email);

        Freelancer incoming = new Freelancer();
        incoming.setFirstName(req.getFirstName());
        incoming.setLastName(req.getLastName());
        incoming.setPhone(req.getPhone());
        incoming.setTitle(req.getTitle());
        incoming.setBio(req.getBio());
        incoming.setSkills(serializeSkills(req.getSkills()));
        incoming.setHourlyRate(req.getHourlyRate());
        incoming.setDailyRate(req.getDailyRate());
        incoming.setAvailability(req.getAvailability());
        incoming.setAddress(req.getAddress());
        incoming.setCity(req.getCity());
        incoming.setCountry(req.getCountry());
        incoming.setPostalCode(req.getPostalCode());
        incoming.setProfilePicture(req.getProfilePicture());
        incoming.setPortfolioUrl(req.getPortfolioUrl());
        incoming.setLinkedinUrl(req.getLinkedinUrl());
        incoming.setGithubUrl(req.getGithubUrl());
        incoming.setDateOfBirth(req.getDateOfBirth());
        incoming.setGender(req.getGender());
        incoming.setIsAvailable(req.getIsAvailable());

        Freelancer updated = freelancerService.updateFreelancerProfile(me.getId(), incoming);
        FreelancerDto dto = freelancerService.convertToDto(updated);
        return ResponseEntity.ok(MessageResponse.success("Freelancer profile updated successfully", dto));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createFreelancer(@RequestBody Freelancer freelancer) {
        Freelancer createdFreelancer = freelancerService.createFreelancer(freelancer);
        return ResponseEntity.ok(MessageResponse.success("Freelancer created successfully", createdFreelancer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getFreelancerById(@PathVariable Long id) {
        Freelancer freelancer = freelancerService.getFreelancerById(id);
        return ResponseEntity.ok(MessageResponse.success("Freelancer retrieved successfully", freelancer));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllFreelancers(Pageable pageable) {
        Page<Freelancer> freelancers = freelancerService.getAllFreelancers(pageable);
        return ResponseEntity.ok(MessageResponse.success("Freelancers retrieved successfully", freelancers));
    }

    @GetMapping("/verified")
    public ResponseEntity<MessageResponse> getVerifiedFreelancers() {
        List<Freelancer> freelancers = freelancerService.getVerifiedFreelancers();
        return ResponseEntity.ok(MessageResponse.success("Verified freelancers retrieved successfully", freelancers));
    }

    @GetMapping("/available")
    public ResponseEntity<MessageResponse> getAvailableFreelancers() {
        List<Freelancer> freelancers = freelancerService.getAvailableFreelancers();
        return ResponseEntity.ok(MessageResponse.success("Available freelancers retrieved successfully", freelancers));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<MessageResponse> getTopRatedFreelancers() {
        List<Freelancer> freelancers = freelancerService.getTopRatedFreelancers();
        return ResponseEntity.ok(MessageResponse.success("Top rated freelancers retrieved successfully", freelancers));
    }

    @GetMapping("/search")
    public ResponseEntity<MessageResponse> searchFreelancers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal minRate,
            @RequestParam(required = false) BigDecimal maxRate) {
        List<Freelancer> freelancers = freelancerService.searchFreelancers(keyword, city, country, minRate, maxRate);
        return ResponseEntity.ok(MessageResponse.success("Search results retrieved successfully", freelancers));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateFreelancer(@PathVariable Long id, @RequestBody Freelancer freelancer) {
        Freelancer updatedFreelancer = freelancerService.updateFreelancer(id, freelancer);
        return ResponseEntity.ok(MessageResponse.success("Freelancer updated successfully", updatedFreelancer));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<MessageResponse> updateFreelancerProfile(@PathVariable Long id, @RequestBody Freelancer freelancer) {
        Freelancer updatedFreelancer = freelancerService.updateFreelancerProfile(id, freelancer);
        return ResponseEntity.ok(MessageResponse.success("Freelancer profile updated successfully", updatedFreelancer));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<MessageResponse> verifyFreelancer(@PathVariable Long id) {
        Freelancer freelancer = freelancerService.verifyFreelancer(id);
        return ResponseEntity.ok(MessageResponse.success("Freelancer verified successfully", freelancer));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<MessageResponse> updateAvailability(@PathVariable Long id, @RequestParam Boolean isAvailable) {
        Freelancer freelancer = freelancerService.updateAvailability(id, isAvailable);
        return ResponseEntity.ok(MessageResponse.success("Availability updated successfully", freelancer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFreelancer(@PathVariable Long id) {
        freelancerService.deleteFreelancer(id);
        return ResponseEntity.ok(MessageResponse.success("Freelancer deleted successfully"));
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String serializeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }
}
