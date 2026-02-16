package com.towork;


import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.contract.entity.Contrat;
import com.towork.contract.repository.ContratRepository;
import com.towork.contract.service.impl.ContratServiceImpl;
import com.towork.contract.entity.Contrat;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.mission.entity.MissionStatus;
import com.towork.user.entity.Client;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.towork.contract.entity.EtatContrat;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;

@ExtendWith(MockitoExtension.class)
class ContratServiceImplTest {

    @Mock private ContratRepository contratRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private com.towork.user.repository.ClientRepository clientRepository;
    @Mock private com.towork.user.repository.FreelancerRepository freelancerRepository;
    @Mock private MissionRepository missionRepository;

    @InjectMocks
    private ContratServiceImpl service;

    private Client client;
    private Freelancer freelancer;
    private Mission mission;
    private Candidature candidature;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(10L);

        freelancer = new Freelancer();
        freelancer.setId(20L);

        mission = new Mission();
        mission.setId(30L);
        mission.setStatus(MissionStatus.PUBLISHED);

        candidature = new Candidature();
        candidature.setFreelancer(freelancer);
        candidature.setMission(mission);
        candidature.setStatus(CandidatureStatus.ACCEPTED);
    }

    private Contrat newContrat() {
        Contrat contrat = new Contrat();
        contrat.setClient(client);
        contrat.setFreelancer(freelancer);
        contrat.setMission(mission);
        contrat.setTitle("Contrat mission");
        contrat.setDescription("Livraison");
        contrat.setPaymentTerms("NET30");
        contrat.setPaymentTerms("NET30");
        contrat.setTotalAmount(new BigDecimal("2000"));
        contrat.setStartDate(LocalDate.now());
        contrat.setEndDate(LocalDate.now().plusDays(30));
        contrat.setIsActive(true);
        return contrat;
    }

    @Test
    @DisplayName("createContrat valide la candidature ACCEPTED et initialise en DRAFT")
    void create_ok() {
        Contrat contrat = newContrat();

        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.of(candidature));
        when(contratRepository.findByClientAndFreelancer(client, freelancer)).thenReturn(List.of());
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));

        Contrat saved = service.createContrat(contrat);

        assertThat(saved.getEtat()).isEqualTo(EtatContrat.DRAFT);
        verify(contratRepository).save(saved);
    }

    @Test
    @DisplayName("createContrat échoue sans candidature ACCEPTED ou si déjà existant")
    void create_requiresAcceptedCandidature() {
        Contrat contrat = newContrat();

        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createContrat(contrat))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("candidature");

        candidature.setStatus(CandidatureStatus.PENDING);
        when(candidatureRepository.findByFreelancerAndMission(freelancer, mission)).thenReturn(Optional.of(candidature));

        assertThatThrownBy(() -> service.createContrat(contrat))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non-accepted");

        when(contratRepository.findByClientAndFreelancer(client, freelancer))
                .thenReturn(List.of(newContrat()));
        candidature.setStatus(CandidatureStatus.ACCEPTED);

        assertThatThrownBy(() -> service.createContrat(contrat))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateContrat refuse les contrats terminés ou annulés")
    void update_guardrails() {
        Contrat existing = newContrat();
        existing.setEtat(EtatContrat.COMPLETED);
        when(contratRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateContrat(1L, newContrat()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updateContratEtat met à jour le statut et la mission associée")
    void updateState_updatesMission() {
        Contrat existing = newContrat();
        existing.setEtat(EtatContrat.DRAFT);

        when(contratRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(contratRepository.save(any(Contrat.class))).thenAnswer(inv -> inv.getArgument(0));
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        Contrat activated = service.updateContratEtat(2L, EtatContrat.ACTIVE);
        assertThat(activated.getEtat()).isEqualTo(EtatContrat.ACTIVE);
        assertThat(activated.getMission().getStatus()).isEqualTo(MissionStatus.IN_PROGRESS);

        Contrat completed = service.updateContratEtat(2L, EtatContrat.COMPLETED);
        assertThat(completed.getMission().getStatus()).isEqualTo(MissionStatus.COMPLETED);

        existing.setEtat(EtatContrat.CANCELLED);
        assertThatThrownBy(() -> service.updateContratEtat(2L, EtatContrat.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reactivate");
    }

    @Test
    @DisplayName("getters propagent les ResourceNotFound et délèguent aux repositories")
    void repositoryDelegations() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(freelancerRepository.findById(20L)).thenReturn(Optional.of(freelancer));
        when(contratRepository.findByClient(client)).thenReturn(List.of(newContrat()));
        when(contratRepository.findByFreelancer(freelancer)).thenReturn(List.of(newContrat()));
        when(contratRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(newContrat())));

        assertThat(service.getContratsByClient(10L)).hasSize(1);
        assertThat(service.getContratsByFreelancer(20L)).hasSize(1);
        assertThat(service.getAllContrats(Pageable.unpaged()).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getContratById lève ResourceNotFoundException si absent")
    void getContratById_missing() {
        when(contratRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContratById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }
}