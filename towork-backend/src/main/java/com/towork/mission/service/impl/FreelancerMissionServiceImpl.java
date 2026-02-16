package com.towork.mission.service.impl;

import com.towork.exception.BusinessException;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.milestone.dto.MilestoneDto;
import com.towork.mission.dto.FreelancerMissionWithMilestonesDto;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.towork.mission.dto.FreelancerMilestoneDto;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.mission.service.FreelancerMissionService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreelancerMissionServiceImpl implements FreelancerMissionService {

    private final MissionRepository missionRepository;
    private final FreelancerRepository freelancerRepository;
    private final MilestoneRepository milestoneRepository;

    @Override
    public List<FreelancerMissionWithMilestonesDto> getMissionsForFreelancer(String freelancerEmail) {
        Freelancer freelancer = freelancerRepository.findActiveByEmail(freelancerEmail)
                .orElseThrow(() -> new BusinessException("Freelancer not found or inactive"));

        List<Mission> missions = missionRepository.findActiveByFreelancer(freelancer);

        return missions.stream()
                .map(mission -> {
                    var milestones = milestoneRepository.findByMissionAndIsActiveTrueOrderByOrderIndexAsc(mission)
                            .stream()
                            .map(ms -> new FreelancerMilestoneDto(
                                    ms.getId(),
                                    ms.getTitle(),
                                    ms.getDescription(),
                                    ms.getAmount(),
                                    ms.getDueDate(),
                                    ms.getStatus() != null ? ms.getStatus().name() : null,
                                    ms.getIsCompleted(),
                                    ms.getPaidAt()
                            ))
                            .toList();

                    BigDecimal totalAmount = milestones.stream()
                            .map(FreelancerMilestoneDto::amount)
                            .filter(a -> a != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String clientName = null;
                    if (mission.getClient() != null) {
                        String first = mission.getClient().getFirstName();
                        String last = mission.getClient().getLastName();
                        String company = mission.getClient().getCompanyName();
                        if (company != null && !company.isBlank()) {
                            clientName = company;
                        } else if (first != null || last != null) {
                            clientName = String.join(" ",
                                    first != null ? first : "",
                                    last != null ? last : "").trim();
                        } else {
                            clientName = mission.getClient().getEmail();
                        }
                    }

                    return new FreelancerMissionWithMilestonesDto(
                            mission.getId(),
                            mission.getTitle(),
                            mission.getDescription(),
                            mission.getStatus() != null ? mission.getStatus().name() : null,
                            totalAmount,
                            clientName,
                            mission.getCreatedAt(),
                            mission.getUpdatedAt(),
                            milestones
                    );
                })
                .toList();
    }
}
