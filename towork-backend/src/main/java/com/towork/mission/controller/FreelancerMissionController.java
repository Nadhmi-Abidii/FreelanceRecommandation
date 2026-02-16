package com.towork.mission.controller;

import com.towork.config.MessageResponse;
import com.towork.mission.dto.FreelancerMissionWithMilestonesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.towork.mission.service.FreelancerMissionService;
import com.towork.user.entity.Freelancer;

@RestController
@RequestMapping("/freelancer")
@RequiredArgsConstructor
public class FreelancerMissionController {

    private final FreelancerMissionService freelancerMissionService;

    @GetMapping("/missions")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> getMyMissions(Authentication authentication) {
        String email = authentication.getName();
        List<FreelancerMissionWithMilestonesDto> missions = freelancerMissionService.getMissionsForFreelancer(email);
        return ResponseEntity.ok(MessageResponse.success("Freelancer missions retrieved successfully", missions));
    }
}
