package com.towork.user.service.impl;

import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Freelancer;
import com.towork.user.dto.FreelancerDto;
import com.towork.user.repository.FreelancerRepository;
import com.towork.user.service.FreelancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreelancerServiceImpl implements FreelancerService {

    private final FreelancerRepository freelancerRepository;

    @Override
    public Freelancer createFreelancer(Freelancer freelancer) {
        return freelancerRepository.save(freelancer);
    }

    @Override
    public Freelancer updateFreelancer(Long id, Freelancer freelancer) {
        Freelancer existingFreelancer = getFreelancerById(id);
        freelancer.setId(existingFreelancer.getId());
        freelancer.setCreatedAt(existingFreelancer.getCreatedAt());
        return freelancerRepository.save(freelancer);
    }

    @Override
    public void deleteFreelancer(Long id) {
        Freelancer freelancer = getFreelancerById(id);
        freelancerRepository.deleteById(id);
        
    }

    @Override
    public Freelancer getFreelancerById(Long id) {
        return freelancerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + id));
    }

    @Override
    public Freelancer getFreelancerByEmail(String email) {
        return freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with email: " + email));
    }

    @Override
    public List<Freelancer> getAllFreelancers() {
        return freelancerRepository.findAll();
    }

    @Override
    public Page<Freelancer> getAllFreelancers(Pageable pageable) {
        return freelancerRepository.findAll(pageable);
    }

    @Override
    public List<Freelancer> getVerifiedFreelancers() {
        return freelancerRepository.findByIsVerified(true);
    }

    @Override
    public List<Freelancer> getAvailableFreelancers() {
        return freelancerRepository.findActiveAvailableFreelancers();
    }

    @Override
    public List<Freelancer> getTopRatedFreelancers() {
        return freelancerRepository.findTopRatedFreelancers();
    }

    @Override
    public List<Freelancer> searchFreelancers(String keyword, String city, String country, BigDecimal minRate, BigDecimal maxRate) {
        // This would need a custom query implementation
        return freelancerRepository.findAll();
    }

    @Override
    public FreelancerDto convertToDto(Freelancer freelancer) {
        FreelancerDto dto = new FreelancerDto();
        dto.setId(freelancer.getId());
        dto.setFirstName(freelancer.getFirstName());
        dto.setLastName(freelancer.getLastName());
        dto.setEmail(freelancer.getEmail());
        dto.setPhone(freelancer.getPhone());
        dto.setTitle(freelancer.getTitle());
        dto.setBio(freelancer.getBio());
        dto.setSkills(parseSkills(freelancer.getSkills()));
        dto.setHourlyRate(freelancer.getHourlyRate());
        dto.setDailyRate(freelancer.getDailyRate());
        dto.setAvailability(freelancer.getAvailability());
        dto.setAddress(freelancer.getAddress());
        dto.setCity(freelancer.getCity());
        dto.setCountry(freelancer.getCountry());
        dto.setPostalCode(freelancer.getPostalCode());
        dto.setProfilePicture(freelancer.getProfilePicture());
        dto.setPortfolioUrl(freelancer.getPortfolioUrl());
        dto.setLinkedinUrl(freelancer.getLinkedinUrl());
        dto.setGithubUrl(freelancer.getGithubUrl());
        dto.setDateOfBirth(freelancer.getDateOfBirth());
        dto.setGender(freelancer.getGender());
        dto.setIsVerified(freelancer.getIsVerified());
        dto.setIsAvailable(freelancer.getIsAvailable());
        dto.setRating(freelancer.getRating());
        dto.setTotalProjects(freelancer.getTotalProjects());
        dto.setSuccessRate(freelancer.getSuccessRate());
        dto.setCreatedAt(freelancer.getCreatedAt());
        dto.setUpdatedAt(freelancer.getUpdatedAt());
        return dto;
    }

    @Override
    public Freelancer convertToEntity(FreelancerDto freelancerDto) {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(freelancerDto.getId());
        freelancer.setFirstName(freelancerDto.getFirstName());
        freelancer.setLastName(freelancerDto.getLastName());
        freelancer.setEmail(freelancerDto.getEmail());
        freelancer.setPhone(freelancerDto.getPhone());
        freelancer.setTitle(freelancerDto.getTitle());
        freelancer.setBio(freelancerDto.getBio());
        freelancer.setSkills(serializeSkills(freelancerDto.getSkills()));
        freelancer.setHourlyRate(freelancerDto.getHourlyRate());
        freelancer.setDailyRate(freelancerDto.getDailyRate());
        freelancer.setAvailability(freelancerDto.getAvailability());
        freelancer.setAddress(freelancerDto.getAddress());
        freelancer.setCity(freelancerDto.getCity());
        freelancer.setCountry(freelancerDto.getCountry());
        freelancer.setPostalCode(freelancerDto.getPostalCode());
        freelancer.setProfilePicture(freelancerDto.getProfilePicture());
        freelancer.setPortfolioUrl(freelancerDto.getPortfolioUrl());
        freelancer.setLinkedinUrl(freelancerDto.getLinkedinUrl());
        freelancer.setGithubUrl(freelancerDto.getGithubUrl());
        freelancer.setDateOfBirth(freelancerDto.getDateOfBirth());
        freelancer.setGender(freelancerDto.getGender());
        freelancer.setIsVerified(freelancerDto.getIsVerified());
        freelancer.setIsAvailable(freelancerDto.getIsAvailable());
        freelancer.setRating(freelancerDto.getRating());
        freelancer.setTotalProjects(freelancerDto.getTotalProjects());
        freelancer.setSuccessRate(freelancerDto.getSuccessRate());
        return freelancer;
    }

    @Override
    public Freelancer verifyFreelancer(Long id) {
        Freelancer freelancer = getFreelancerById(id);
        freelancer.setIsVerified(true);
        return freelancerRepository.save(freelancer);
    }

    @Override
    public Freelancer updateFreelancerProfile(Long id, Freelancer freelancer) {
        Freelancer existingFreelancer = getFreelancerById(id);
        copyIfNotBlank(freelancer.getFirstName(), existingFreelancer::setFirstName);
        copyIfNotBlank(freelancer.getLastName(), existingFreelancer::setLastName);
        copyIfNotBlank(freelancer.getPhone(), existingFreelancer::setPhone);
        copyIfNotBlank(freelancer.getTitle(), existingFreelancer::setTitle);
        copyIfNotBlank(freelancer.getBio(), existingFreelancer::setBio);
        copyIfNotBlank(freelancer.getSkills(), existingFreelancer::setSkills);

        copyIfNotNull(freelancer.getHourlyRate(), existingFreelancer::setHourlyRate);
        copyIfNotNull(freelancer.getDailyRate(), existingFreelancer::setDailyRate);

        copyIfNotBlank(freelancer.getAvailability(), existingFreelancer::setAvailability);
        copyIfNotBlank(freelancer.getAddress(), existingFreelancer::setAddress);
        copyIfNotBlank(freelancer.getCity(), existingFreelancer::setCity);
        copyIfNotBlank(freelancer.getCountry(), existingFreelancer::setCountry);
        copyIfNotBlank(freelancer.getPostalCode(), existingFreelancer::setPostalCode);
        copyIfNotBlank(freelancer.getProfilePicture(), existingFreelancer::setProfilePicture);
        copyIfNotBlank(freelancer.getPortfolioUrl(), existingFreelancer::setPortfolioUrl);
        copyIfNotBlank(freelancer.getLinkedinUrl(), existingFreelancer::setLinkedinUrl);
        copyIfNotBlank(freelancer.getGithubUrl(), existingFreelancer::setGithubUrl);

        copyIfNotNull(freelancer.getDateOfBirth(), existingFreelancer::setDateOfBirth);
        copyIfNotBlank(freelancer.getGender(), existingFreelancer::setGender);
        if (freelancer.getIsAvailable() != null) {
            existingFreelancer.setIsAvailable(freelancer.getIsAvailable());
        }
        return freelancerRepository.save(existingFreelancer);
    }

    @Override
    public Freelancer updateAvailability(Long id, Boolean isAvailable) {
        Freelancer freelancer = getFreelancerById(id);
        freelancer.setIsAvailable(isAvailable);
        return freelancerRepository.save(freelancer);
    }

    private List<String> parseSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
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

    private void copyIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private <T> void copyIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
