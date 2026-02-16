package com.towork.user.service.impl;

import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Competence;
import com.towork.user.dto.CompetenceDto;
import com.towork.user.repository.CompetenceRepository;
import com.towork.user.service.CompetenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetenceServiceImpl implements CompetenceService {

    private final CompetenceRepository competenceRepository;

    @Override
    public Competence createCompetence(Competence competence) {
        return competenceRepository.save(competence);
    }

    @Override
    public Competence updateCompetence(Long id, Competence competence) {
        Competence existingCompetence = getCompetenceById(id);
        competence.setId(existingCompetence.getId());
        competence.setCreatedAt(existingCompetence.getCreatedAt());
        return competenceRepository.save(competence);
    }

    @Override
    public void deleteCompetence(Long id) {
        Competence competence = getCompetenceById(id);
        competence.setIsActive(false);
        competenceRepository.save(competence);
    }

    @Override
    public Competence getCompetenceById(Long id) {
        return competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competence not found with id: " + id));
    }

    @Override
    public List<Competence> getCompetencesByFreelancer(Long freelancerId) {
        return competenceRepository.findActiveByFreelancer(null); // Would need freelancer entity
    }

    @Override
    public List<Competence> getAllCompetences() {
        return competenceRepository.findAll();
    }

    @Override
    public Page<Competence> getAllCompetences(Pageable pageable) {
        return competenceRepository.findAll(pageable);
    }

    @Override
    public List<Competence> getCompetencesByName(String name) {
        return competenceRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Competence> getCompetencesByLevel(String level) {
        return competenceRepository.findByLevel(level);
    }

    @Override
    public List<Competence> getCertifiedCompetences() {
        return competenceRepository.findByIsCertified(true);
    }

    @Override
    public List<String> getDistinctCompetenceNames() {
        return competenceRepository.findDistinctCompetenceNames();
    }

    @Override
    public CompetenceDto convertToDto(Competence competence) {
        return new CompetenceDto(
                competence.getId(),
                null, // freelancerId would need to be set
                null, // freelancerName would need to be set
                competence.getName(),
                competence.getDescription(),
                competence.getLevel(),
                competence.getYearsOfExperience(),
                competence.getIsCertified(),
                competence.getCertificationName(),
                competence.getCertificationDate(),
                competence.getCreatedAt(),
                competence.getUpdatedAt()
        );
    }

    @Override
    public Competence convertToEntity(CompetenceDto competenceDto) {
        Competence competence = new Competence();
        competence.setId(competenceDto.getId());
        competence.setName(competenceDto.getName());
        competence.setDescription(competenceDto.getDescription());
        competence.setLevel(competenceDto.getLevel());
        competence.setYearsOfExperience(competenceDto.getYearsOfExperience());
        competence.setIsCertified(competenceDto.getIsCertified());
        competence.setCertificationName(competenceDto.getCertificationName());
        competence.setCertificationDate(competenceDto.getCertificationDate());
        return competence;
    }

    @Override
    public Competence certifyCompetence(Long id, String certificationName) {
        Competence competence = getCompetenceById(id);
        competence.setIsCertified(true);
        competence.setCertificationName(certificationName);
        competence.setCertificationDate(LocalDate.now());
        return competenceRepository.save(competence);
    }
}
