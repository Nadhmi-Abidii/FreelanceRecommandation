package com.towork;

import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Competence;
import com.towork.user.dto.CompetenceDto;
import com.towork.user.repository.CompetenceRepository;
import com.towork.user.service.impl.CompetenceServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetenceServiceImplTest {

    @Mock private CompetenceRepository competenceRepository;

    @InjectMocks
    private CompetenceServiceImpl service;

    private Competence competence() {
        Competence competence = new Competence();
        competence.setId(1L);
        competence.setName("Java");
        competence.setLevel("EXPERT");
        competence.setIsActive(true);
        
        return competence;
    }

    @Test
    @DisplayName("updateCompetence conserve id et createdAt")
    void update_preservesIdentity() {
        Competence stored = competence();
        when(competenceRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(competenceRepository.save(any(Competence.class))).thenAnswer(inv -> inv.getArgument(0));

        Competence updates = competence();
        updates.setName("Spring");

        Competence saved = service.updateCompetence(1L, updates);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isEqualTo(stored.getCreatedAt());
        assertThat(saved.getName()).isEqualTo("Spring");
    }

    @Test
    @DisplayName("certifyCompetence marque certifié et date du jour")
    void certifyCompetence_setsFields() {
        Competence stored = competence();
        when(competenceRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(competenceRepository.save(any(Competence.class))).thenAnswer(inv -> inv.getArgument(0));

        Competence certified = service.certifyCompetence(1L, "AWS");

        assertThat(certified.getIsCertified()).isTrue();
        assertThat(certified.getCertificationName()).isEqualTo("AWS");
        assertThat(certified.getCertificationDate()).isNotNull();
    }

    @Test
    @DisplayName("convertToDto et convertToEntity mappent les champs principaux")
    void conversions_work() {
        Competence competence = competence();
        competence.setDescription("Backend");
        competence.setCertificationName("Oracle");
        competence.setCertificationDate(LocalDate.now());
        competence.setYearsOfExperience(5);
        competence.setIsCertified(true);

        CompetenceDto dto = service.convertToDto(competence);
        assertThat(dto.getName()).isEqualTo("Java");
        assertThat(dto.getLevel()).isEqualTo("EXPERT");
        assertThat(dto.getCertificationName()).isEqualTo("Oracle");

        Competence entity = service.convertToEntity(dto);
        assertThat(entity.getName()).isEqualTo("Java");
        assertThat(entity.getDescription()).isEqualTo("Backend");
    }

    @Test
    @DisplayName("getCompetenceById lève ResourceNotFoundException si absent")
    void getCompetenceById_missing() {
        when(competenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompetenceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("finders et pagination délèguent au repository")
    void repositoryDelegations() {
        Competence competence = competence();
        when(competenceRepository.findAll()).thenReturn(List.of(competence));
        when(competenceRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(competence)));
        when(competenceRepository.findByNameContainingIgnoreCase("Java")).thenReturn(List.of(competence));
        when(competenceRepository.findByLevel("EXPERT")).thenReturn(List.of(competence));
        when(competenceRepository.findByIsCertified(true)).thenReturn(List.of(competence));
        when(competenceRepository.findDistinctCompetenceNames()).thenReturn(List.of("Java"));

        assertThat(service.getAllCompetences()).hasSize(1);
        assertThat(service.getAllCompetences(Pageable.unpaged()).getContent()).hasSize(1);
        assertThat(service.getCompetencesByName("Java")).hasSize(1);
        assertThat(service.getCompetencesByLevel("EXPERT")).hasSize(1);
        assertThat(service.getCertifiedCompetences()).hasSize(1);
        assertThat(service.getDistinctCompetenceNames()).containsExactly("Java");
    }
}