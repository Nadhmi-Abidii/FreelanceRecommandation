package com.towork.portfolio.service;

import com.towork.exception.BusinessException;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.towork.portfolio.dto.FreelancerPortfolioDto;
import com.towork.portfolio.dto.PaidMilestoneDto;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final MilestoneRepository milestoneRepository;
    private final FreelancerRepository freelancerRepository;

    @Transactional(readOnly = true)
    public FreelancerPortfolioDto getPortfolioForFreelancer(String email) {
        Freelancer freelancer = freelancerRepository.findActiveByEmail(email)
                .orElseThrow(() -> new BusinessException("Freelancer not found or inactive"));

        List<Milestone> paidMilestones = milestoneRepository.findPaidByFreelancer(freelancer);
        List<PaidMilestoneDto> dtos = paidMilestones.stream()
                .map(m -> new PaidMilestoneDto(
                        m.getId(),
                        m.getMission() != null ? m.getMission().getTitle() : null,
                        m.getTitle(),
                        m.getAmount(),
                        m.getPaidAt()
                ))
                .toList();

        BigDecimal total = dtos.stream()
                .map(PaidMilestoneDto::amount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FreelancerPortfolioDto(total, dtos);
    }
}
