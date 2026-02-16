package com.towork;

import com.towork.candidature.repository.CandidatureRepository;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneStatus;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.DomaineRepository;
import com.towork.wallet.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.towork.mission.entity.BudgetType;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.TypeTravail;
import com.towork.mission.repository.MissionRepository;
import com.towork.mission.service.impl.MissionServiceImpl;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class MissionServiceImplTest {

   @Mock private MissionRepository missionRepository;
   @Mock private ClientRepository clientRepository;
   @Mock private DomaineRepository domaineRepository;
   @Mock private CandidatureRepository candidatureRepository;
   @Mock private MilestoneRepository milestoneRepository;
   @Mock private PaymentService paymentService;

   @InjectMocks
   private MissionServiceImpl service;

   private Client client;
   private Domaine domaine;

   @BeforeEach
   void setUp() {
       client = new Client();
       client.setId(10L);

       domaine = new Domaine();
       domaine.setId(20L);
       domaine.setIsActive(true);
   }

   private Mission newMissionDraft() {
       Mission m = new Mission();
       m.setClient(client);
       m.setDomaine(domaine);
       m.setTitle("Build a Spring service");
       m.setDescription("Write unit tests and business rules");
       m.setRequirements("JUnit 5, Mockito");
       m.setBudgetMin(new BigDecimal("100.00"));
       m.setBudgetMax(new BigDecimal("300.00"));
       m.setBudgetType(BudgetType.FIXED);
       m.setTypeTravail(TypeTravail.REMOTE);
       m.setDeadline(LocalDate.now().plusDays(7));
       m.setEstimatedDuration(5);
       m.setSkillsRequired("Java, Spring, JPA");
       m.setIsUrgent(false);
       return m;
   }

   @Nested
   class CreateMission {

       @Test
       @DisplayName("OK: crée mission et la publie quand client & domaine valides")
       void create_ok() {
           Mission m = newMissionDraft();

           when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
           when(domaineRepository.findById(20L)).thenReturn(Optional.of(domaine));
           when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

           Mission saved = service.createMission(m);

           assertThat(saved.getStatus()).isEqualTo(MissionStatus.PUBLISHED);
           verify(missionRepository).save(saved);
       }

       @Test
       @DisplayName("KO: client introuvable -> ResourceNotFoundException")
       void create_clientNotFound() {
           Mission m = newMissionDraft();
           when(clientRepository.findById(10L)).thenReturn(Optional.empty());

           assertThatThrownBy(() -> service.createMission(m))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("Client not found");

           verifyNoInteractions(domaineRepository);
           verify(missionRepository, never()).save(any());
       }

       @Test
       @DisplayName("KO: domaine inactif -> BusinessException")
       void create_inactiveDomaine() {
           Mission m = newMissionDraft();

           Domaine inactive = new Domaine();
           inactive.setId(20L);
           inactive.setIsActive(false);

           when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
           when(domaineRepository.findById(20L)).thenReturn(Optional.of(inactive));

           assertThatThrownBy(() -> service.createMission(m))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("inactive domaine");

           verify(missionRepository, never()).save(any());
       }

       @Test
       @DisplayName("KO: budgetMin > budgetMax -> BusinessException")
       void create_badBudgetRange() {
           Mission m = newMissionDraft();
           m.setBudgetMin(new BigDecimal("500"));
           m.setBudgetMax(new BigDecimal("100"));

           when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
           when(domaineRepository.findById(20L)).thenReturn(Optional.of(domaine));

           assertThatThrownBy(() -> service.createMission(m))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("Minimum budget cannot be greater");

           verify(missionRepository, never()).save(any());
       }
   }

   @Nested
   class UpdateStatus {

       @Test
       @DisplayName("OK: passer en PUBLISHED si champs requis présents")
       void publish_ok() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.DRAFT);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));
           when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

           Mission out = service.updateMissionStatus(1L, MissionStatus.PUBLISHED);

           assertThat(out.getStatus()).isEqualTo(MissionStatus.PUBLISHED);
           verify(missionRepository).save(m);
       }

       @Test
       @DisplayName("KO: publish sans title -> BusinessException")
       void publish_missingTitle() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.DRAFT);
           m.setTitle("  "); // blank

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.updateMissionStatus(1L, MissionStatus.PUBLISHED))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("title is required");
       }

       @Test
       @DisplayName("KO: publish sans description -> BusinessException")
       void publish_missingDescription() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.DRAFT);
           m.setDescription(null);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.updateMissionStatus(1L, MissionStatus.PUBLISHED))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("description is required");
       }

       @Test
       @DisplayName("KO: publish sans budget -> BusinessException")
       void publish_missingBudget() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.DRAFT);
           m.setBudgetMin(null);
           m.setBudgetMax(null);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.updateMissionStatus(1L, MissionStatus.PUBLISHED))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("Budget information is required");
       }

       @Test
       @DisplayName("KO: mission COMPLETED -> changement interdit")
       void completed_cannotChange() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.COMPLETED);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.updateMissionStatus(1L, MissionStatus.PUBLISHED))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("completed");
       }

       @Test
       @DisplayName("KO: mission CANCELLED -> réactivation interdite")
       void cancelled_cannotReactivate() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.CANCELLED);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.updateMissionStatus(1L, MissionStatus.DRAFT))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("cancelled");
       }
   }

   @Nested
   class DeleteMission {

       @Test
       @DisplayName("OK: delete autorisé seulement en DRAFT (désactivation)")
       void delete_ok_whenDraft() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.DRAFT);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           service.deleteMission(1L);

           assertThat(m.getIsActive()).isFalse();
           verify(missionRepository).save(m);
       }

       @Test
       @DisplayName("KO: delete non-DRAFT -> BusinessException")
       void delete_notDraft() {
           Mission m = newMissionDraft();
           m.setId(1L);
           m.setStatus(MissionStatus.PUBLISHED);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(m));

           assertThatThrownBy(() -> service.deleteMission(1L))
                   .isInstanceOf(BusinessException.class)
                   .hasMessageContaining("not in draft");
       }
   }

   @Nested
   class FindersAndPaging {

       @Test
       @DisplayName("getMissionById -> not found")
       void getById_notFound() {
           when(missionRepository.findById(99L)).thenReturn(Optional.empty());
           assertThatThrownBy(() -> service.getMissionById(99L))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("99");
       }

       @Test
       @DisplayName("getMissionsByClient -> client not found")
       void getByClient_clientNotFound() {
           when(clientRepository.findById(123L)).thenReturn(Optional.empty());
           assertThatThrownBy(() -> service.getMissionsByClient(123L))
                   .isInstanceOf(ResourceNotFoundException.class);
       }

       @Test
       @DisplayName("getMissionsByDomaine -> domaine not found")
       void getByDomaine_notFound() {
           when(domaineRepository.findById(456L)).thenReturn(Optional.empty());
           assertThatThrownBy(() -> service.getMissionsByDomaine(456L))
                   .isInstanceOf(ResourceNotFoundException.class);
       }

       @Test
       @DisplayName("getAllMissions -> paginé")
       void getAll_paged() {
           when(missionRepository.findAll(any(Pageable.class)))
                   .thenReturn(new PageImpl<>(List.of(newMissionDraft())));

           var page = service.getAllMissions(Pageable.ofSize(10));

           assertThat(page.getTotalElements()).isEqualTo(1);
       }
   }

   @Nested
   class FinalizationFlow {

       @Test
       @DisplayName("Freelancer peut soumettre la livraison finale quand tous les jalons sont validés/payés")
       void submitFinalDelivery_ok() {
           Mission mission = newMissionDraft();
           mission.setId(1L);
           mission.setStatus(MissionStatus.IN_PROGRESS);

           Freelancer freelancer = new Freelancer();
           freelancer.setId(5L);
           freelancer.setEmail("free@towork.test");
           mission.setAssignedFreelancer(freelancer);

           Milestone milestone = new Milestone();
           milestone.setId(11L);
           milestone.setStatus(MilestoneStatus.PAID);

           when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
           when(milestoneRepository.findByMissionAndIsActiveTrue(mission)).thenReturn(List.of(milestone));
           when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

           var user = User.withUsername(freelancer.getEmail()).password("pwd").roles("FREELANCER").build();

           Mission updated = service.submitFinalDelivery(1L, user);

           assertThat(updated.getStatus()).isEqualTo(MissionStatus.PENDING_CLOSURE);
           verify(missionRepository).save(mission);
       }

       @Test
       @DisplayName("Client clôture la mission et paye les jalons restants")
       void closeMission_triggersPaymentAndCompletion() {
           Mission mission = newMissionDraft();
           mission.setId(2L);
           mission.setStatus(MissionStatus.PENDING_CLOSURE);
           mission.setClient(client);

           Freelancer freelancer = new Freelancer();
           freelancer.setId(8L);
           freelancer.setEmail("free2@towork.test");
           mission.setAssignedFreelancer(freelancer);

           Milestone toPay = new Milestone();
           toPay.setId(21L);
           toPay.setStatus(MilestoneStatus.COMPLETED);

           Milestone alreadyPaid = new Milestone();
           alreadyPaid.setId(22L);
           alreadyPaid.setStatus(MilestoneStatus.PAID);

           when(missionRepository.findById(2L)).thenReturn(Optional.of(mission));
           when(milestoneRepository.findByMissionAndIsActiveTrue(mission)).thenReturn(List.of(toPay, alreadyPaid));
           when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

           client.setEmail("client@test.fr");
           var clientUser = User.withUsername(client.getEmail()).password("pwd").roles("CLIENT").build();

           Mission closed = service.closeMission(2L, clientUser);

           assertThat(closed.getStatus()).isEqualTo(MissionStatus.COMPLETED);
           verify(paymentService).payMilestone(toPay.getId(), client.getId(), freelancer.getId(), "WALLET", "Paiement final mission " + mission.getTitle());
           verify(missionRepository, atLeastOnce()).save(mission);
       }
   }
}
