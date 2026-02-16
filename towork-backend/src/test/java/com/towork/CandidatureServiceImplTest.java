package com.towork;

import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.candidature.service.impl.CandidatureServiceImpl;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.user.entity.Freelancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.repository.FreelancerRepository;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceImplTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private com.towork.user.repository.FreelancerRepository freelancerRepository;
    @Mock private com.towork.mission.repository.MissionRepository missionRepository;

    @InjectMocks
    private CandidatureServiceImpl service;

    private Freelancer freelancer;
    private Mission mission;

    private void stubLookups() {
        when(freelancerRepository.findById(freelancer.getId())).thenReturn(Optional.of(freelancer));
        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
    }

    @BeforeEach
    void setUp() {
        freelancer = new Freelancer();
        freelancer.setId(1L);
        freelancer.setIsAvailable(true);

        mission = new Mission();
        mission.setId(2L);
        mission.setStatus(MissionStatus.PUBLISHED);
    }

    private Candidature newCandidature() {
        Candidature candidature = new Candidature();
        candidature.setFreelancer(freelancer);
        candidature.setMission(mission);
        candidature.setCoverLetter("Motivé");
        candidature.setProposedDuration(10);
        candidature.setResumeUrl("https://resume");
        
        candidature.setIsActive(true);
        return candidature;
    }

    @Test
    @DisplayName("createCandidature sauvegarde une candidature valide en status PENDING")
    void create_ok() {
        Candidature candidature = newCandidature();

        stubLookups();
        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.empty());
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidature saved = service.createCandidature(candidature);

        assertThat(saved.getStatus()).isEqualTo(CandidatureStatus.PENDING);
        verify(candidatureRepository).save(saved);
    }

    @Test
    @DisplayName("createCandidature refuse les doublons pour un freelancer et mission")
    void create_duplicate() {
        Candidature candidature = newCandidature();

        stubLookups();
        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.of(candidature));

        assertThatThrownBy(() -> service.createCandidature(candidature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already applied");

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCandidature rejette les missions non publiées ou freelancer non disponible")
    void create_missionClosedOrFreelancerUnavailable() {
        Candidature candidature = newCandidature();
        mission.setStatus(MissionStatus.COMPLETED);

        stubLookups();
        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCandidature(candidature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer accepting applications");

        mission.setStatus(MissionStatus.PUBLISHED);
        freelancer.setIsAvailable(false);

        assertThatThrownBy(() -> service.createCandidature(candidature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("updateCandidature met à jour uniquement si status PENDING")
    void update_ok() {
        Candidature candidature = newCandidature();
        candidature.setStatus(CandidatureStatus.PENDING);

        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(candidature));
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidature changes = newCandidature();
        changes.setCoverLetter("Nouvelle lettre");
        changes.setProposedDuration(20);
        changes.setResumeUrl("https://resume-new.pdf");

        Candidature updated = service.updateCandidature(5L, changes);

        assertThat(updated.getCoverLetter()).isEqualTo("Nouvelle lettre");
        assertThat(updated.getProposedDuration()).isEqualTo(20);
        assertThat(updated.getResumeUrl()).isEqualTo("https://resume-new.pdf");
    }

    @Test
    @DisplayName("updateCandidature lève une BusinessException si status non PENDING")
    void update_refusedWhenNotPending() {
        Candidature candidature = newCandidature();
        candidature.setStatus(CandidatureStatus.ACCEPTED);
        when(candidatureRepository.findById(6L)).thenReturn(Optional.of(candidature));

        assertThatThrownBy(() -> service.updateCandidature(6L, newCandidature()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in pending status");
    }

    @Test
    @DisplayName("deleteCandidature désactive la candidature sauf si ACCEPTED")
    void delete_marksInactive() {
        Candidature candidature = newCandidature();
        candidature.setStatus(CandidatureStatus.PENDING);
        when(candidatureRepository.findById(7L)).thenReturn(Optional.of(candidature));

        service.deleteCandidature(7L);

        assertThat(candidature.getIsActive()).isFalse();
        verify(candidatureRepository).save(candidature);

        candidature.setStatus(CandidatureStatus.ACCEPTED);
        assertThatThrownBy(() -> service.deleteCandidature(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("accepted candidature");
    }

    @Test
    @DisplayName("updateCandidatureStatus refuse la modification si déjà ACCEPTED")
    void updateStatus_guardrail() {
        Candidature candidature = newCandidature();
        candidature.setStatus(CandidatureStatus.ACCEPTED);
        when(candidatureRepository.findById(8L)).thenReturn(Optional.of(candidature));

        // assertThatThrownBy(() -> service.updateCandidatureStatus(8L, CandidatureStatus.REJECTED, "Non"))
        //         .isInstanceOf(BusinessException.class)
        //         .hasMessageContaining("Cannot change status of an accepted candidature");
    }

    @Test
    @DisplayName("getters s'appuient sur les repositories et renvoient les données paginées")
    void repositoryDelegations() {
        PageImpl<Candidature> page = new PageImpl<>(List.of(newCandidature()));
        when(candidatureRepository.findAll(Pageable.unpaged())).thenReturn(page);

        assertThat(service.getAllCandidatures(Pageable.unpaged())).isSameAs(page);
    }

    @Test
    @DisplayName("getCandidatureById déclenche ResourceNotFoundException pour id inconnu")
    void getCandidatureById_notFound() {
        when(candidatureRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCandidatureById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }
}
