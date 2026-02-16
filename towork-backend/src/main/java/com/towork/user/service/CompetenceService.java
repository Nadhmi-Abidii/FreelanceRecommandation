package com.towork.user.service;

import com.towork.user.entity.Competence;
import com.towork.user.dto.CompetenceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompetenceService {
    Competence createCompetence(Competence competence);
    Competence updateCompetence(Long id, Competence competence);
    void deleteCompetence(Long id);
    Competence getCompetenceById(Long id);
    List<Competence> getCompetencesByFreelancer(Long freelancerId);
    List<Competence> getAllCompetences();
    Page<Competence> getAllCompetences(Pageable pageable);
    List<Competence> getCompetencesByName(String name);
    List<Competence> getCompetencesByLevel(String level);
    List<Competence> getCertifiedCompetences();
    List<String> getDistinctCompetenceNames();
    CompetenceDto convertToDto(Competence competence);
    Competence convertToEntity(CompetenceDto competenceDto);
    Competence certifyCompetence(Long id, String certificationName);
}
