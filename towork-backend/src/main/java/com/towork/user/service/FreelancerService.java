package com.towork.user.service;

import com.towork.user.entity.Freelancer;
import com.towork.user.dto.FreelancerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface FreelancerService {
    Freelancer createFreelancer(Freelancer freelancer);
    Freelancer updateFreelancer(Long id, Freelancer freelancer);
    void deleteFreelancer(Long id);
    Freelancer getFreelancerById(Long id);
    Freelancer getFreelancerByEmail(String email);
    List<Freelancer> getAllFreelancers();
    Page<Freelancer> getAllFreelancers(Pageable pageable);
    List<Freelancer> getVerifiedFreelancers();
    List<Freelancer> getAvailableFreelancers();
    List<Freelancer> getTopRatedFreelancers();
    List<Freelancer> searchFreelancers(String keyword, String city, String country, BigDecimal minRate, BigDecimal maxRate);
    FreelancerDto convertToDto(Freelancer freelancer);
    Freelancer convertToEntity(FreelancerDto freelancerDto);
    Freelancer verifyFreelancer(Long id);
    Freelancer updateFreelancerProfile(Long id, Freelancer freelancer);
     Freelancer updateAvailability(Long id, Boolean isAvailable);
    // Freelancer updateAvailability(Long id, Boolean isAvailable);
}
