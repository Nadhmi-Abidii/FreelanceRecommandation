// package com.towork;


// import com.towork.exception.BusinessException;
// import com.towork.exception.ResourceNotFoundException;
// import com.towork.milestone.entity.Milestone;
// import com.towork.milestone.repository.MilestoneRepository;
// import com.towork.milestone.service.impl.MilestoneServiceImpl;
// import com.towork.mission.entity.Mission;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.Pageable;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class MilestoneServiceImplTest {

//     @Mock private MilestoneRepository milestoneRepository;
//     @Mock private com.towork.mission.repository.MissionRepository missionRepository;

//     @InjectMocks
//     private MilestoneServiceImpl service;

//     private Mission mission;

//     @BeforeEach
//     void setUp() {
//         mission = new Mission();
//         mission.setId(100L);
//         mission.setIsActive(true);
//     }

//     private Milestone newMilestone() {
//         Milestone milestone = new Milestone();
//         milestone.setMission(mission);
//         milestone.setTitle("Phase 1");
//         milestone.setAmount(BigDecimal.TEN);
//         milestone.setDueDate(LocalDate.now().plusDays(5));
//         milestone.setOrderIndex(1);
//         milestone.setIsActive(true);
//         return milestone;
//     }

//     @Test
//     @DisplayName("createMilestone vérifie la mission, le montant positif et la date future")
//     void create_ok() {
//         Milestone milestone = newMilestone();
//         when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
//         when(milestoneRepository.save(any(Milestone.class))).thenAnswer(inv -> inv.getArgument(0));

//         Milestone saved = service.createMilestone(milestone);

//         assertThat(saved.getIsCompleted()).isFalse();
//         verify(milestoneRepository).save(saved);
//     }

//     @Test
//     @DisplayName("createMilestone échoue si mission absente/inactive ou montant négatif")
//     void create_guardrails() {
//         Milestone milestone = newMilestone();
//         when(missionRepository.findById(100L)).thenReturn(Optional.empty());

//         assertThatThrownBy(() -> service.createMilestone(milestone))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessageContaining("Mission not found");

//         mission.setIsActive(false);
//         when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));

//         assertThatThrownBy(() -> service.createMilestone(milestone))
//                 .isInstanceOf(BusinessException.class)
//                 .hasMessageContaining("inactive mission");

//         mission.setIsActive(true);
//         milestone.setAmount(BigDecimal.ZERO);
//         when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));

//         assertThatThrownBy(() -> service.createMilestone(milestone))
//                 .isInstanceOf(BusinessException.class)
//                 .hasMessageContaining("amount must be positive");

//         milestone.setAmount(BigDecimal.ONE);
//         milestone.setDueDate(LocalDate.now().minusDays(1));
//         assertThatThrownBy(() -> service.createMilestone(milestone))
//                 .isInstanceOf(BusinessException.class)
//                 .hasMessageContaining("due date cannot be in the past");
//     }

//     @Test
//     @DisplayName("updateMilestone refuse la mise à jour si déjà complété")
//     void update_guardrails() {
//         Milestone existing = newMilestone();
//         existing.setIsCompleted(true);
//         when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));

//         assertThatThrownBy(() -> service.updateMilestone(1L, newMilestone()))
//                 .isInstanceOf(BusinessException.class)
//                 .hasMessageContaining("completed milestone");
//     }

//     @Test
//     @DisplayName("deleteMilestone marque inactif seulement si non terminé")
//     void delete_marksInactive() {
//         Milestone existing = newMilestone();
//         existing.setIsCompleted(false);
//         when(milestoneRepository.findById(2L)).thenReturn(Optional.of(existing));

//         service.deleteMilestone(2L);

//         assertThat(existing.getIsActive()).isFalse();
//         verify(milestoneRepository).save(existing);
//     }

//     @Test
//     @DisplayName("markMilestoneAsCompleted/Pending gère les transitions et validations")
//     void toggle_completion() {
//         Milestone existing = newMilestone();
//         existing.setIsCompleted(false);
//         when(milestoneRepository.findById(3L)).thenReturn(Optional.of(existing));
//         when(milestoneRepository.save(any(Milestone.class))).thenAnswer(inv -> inv.getArgument(0));

//         Milestone completed = service.markMilestoneAsCompleted(3L, "Done");
//         assertThat(completed.getIsCompleted()).isTrue();
//         assertThat(completed.getCompletionNotes()).isEqualTo("Done");

//         when(milestoneRepository.findById(4L)).thenReturn(Optional.of(completed));
//         assertThatThrownBy(() -> service.markMilestoneAsCompleted(4L, "Again"))
//                 .isInstanceOf(BusinessException.class);

//         when(milestoneRepository.findById(5L)).thenReturn(Optional.of(completed));
//         Milestone pending = service.markMilestoneAsPending(5L);
//         assertThat(pending.getIsCompleted()).isFalse();
//         assertThat(pending.getCompletionDate()).isNull();

//         when(milestoneRepository.findById(6L)).thenReturn(Optional.of(pending));
//         assertThatThrownBy(() -> service.markMilestoneAsPending(6L))
//                 .isInstanceOf(BusinessException.class)
//                 .hasMessageContaining("already pending");
//     }

//     @Test
//     @DisplayName("getters et pagination délèguent aux repositories")
//     void repositoryDelegations() {
//         Milestone milestone = newMilestone();
//         when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
//         when(milestoneRepository.findByMission(mission)).thenReturn(List.of(milestone));
//         when(milestoneRepository.findByMissionOrderByOrderIndex(mission)).thenReturn(List.of(milestone));
//         when(milestoneRepository.findByMissionAndIsCompleted(mission, true)).thenReturn(List.of(milestone));
//         when(milestoneRepository.findByMissionAndIsCompleted(mission, false)).thenReturn(List.of(milestone));
//         when(milestoneRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(milestone)));

//         assertThat(service.getMilestonesByMission(100L)).hasSize(1);
//         assertThat(service.getMilestonesByMissionOrdered(100L)).hasSize(1);
//         assertThat(service.getCompletedMilestones(100L)).hasSize(1);
//         assertThat(service.getPendingMilestones(100L)).hasSize(1);
//         assertThat(service.getAllMilestones(Pageable.unpaged()).getContent()).hasSize(1);
//     }

//     @Test
//     @DisplayName("getMilestoneById lève ResourceNotFoundException si absent")
//     void getMilestoneById_missing() {
//         when(milestoneRepository.findById(404L)).thenReturn(Optional.empty());

//         assertThatThrownBy(() -> service.getMilestoneById(404L))
//                 .isInstanceOf(ResourceNotFoundException.class)
//                 .hasMessageContaining("404");
//     }
// }