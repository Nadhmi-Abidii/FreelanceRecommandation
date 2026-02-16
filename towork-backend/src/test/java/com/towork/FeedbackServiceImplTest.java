package com.towork;

import com.towork.exception.BusinessException;
import com.towork.exception.ConflictException;
import com.towork.exception.ForbiddenActionException;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.dto.CreateFeedbackRequest;
import com.towork.user.dto.FeedbackResponse;
import com.towork.user.entity.Client;
import com.towork.user.entity.Feedback;
import com.towork.user.entity.FeedbackDirection;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FeedbackRepository;
import com.towork.user.repository.FreelancerRepository;
import com.towork.user.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private FreelancerRepository freelancerRepository;

    @InjectMocks
    private FeedbackServiceImpl service;

    private Mission mission;
    private Client client;
    private Freelancer freelancer;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(10L);
        client.setEmail("client@towork.test");

        freelancer = new Freelancer();
        freelancer.setId(20L);
        freelancer.setEmail("freelancer@towork.test");

        mission = new Mission();
        mission.setId(1L);
        mission.setClient(client);
        mission.setAssignedFreelancer(freelancer);
        mission.setStatus(MissionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Client can create a feedback for the assigned freelancer")
    void createFeedback_asClient_success() {
        UserDetails user = User.withUsername(client.getEmail())
                .password("pwd").roles("CLIENT").build();

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(clientRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(feedbackRepository.findByMissionIdAndAuthorUserId(1L, client.getId())).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(99L);
            return f;
        });

        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great delivery", null);
        FeedbackResponse response = service.createFeedback(1L, request, user);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getAuthorUserId()).isEqualTo(client.getId());
        assertThat(response.getTargetUserId()).isEqualTo(freelancer.getId());
        assertThat(response.getDirection()).isEqualTo(FeedbackDirection.CLIENT_TO_FREELANCER);
        assertThat(response.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Feedback creation is rejected if mission is not completed")
    void createFeedback_requiresCompletedMission() {
        mission.setStatus(MissionStatus.IN_PROGRESS);
        UserDetails user = User.withUsername(client.getEmail())
                .password("pwd").roles("CLIENT").build();

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));

        CreateFeedbackRequest request = new CreateFeedbackRequest(4, "Ok", null);
        assertThatThrownBy(() -> service.createFeedback(1L, request, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completed");
    }

    @Test
    @DisplayName("Duplicate feedback for the same mission/author triggers ConflictException")
    void createFeedback_duplicateRejected() {
        UserDetails user = User.withUsername(client.getEmail())
                .password("pwd").roles("CLIENT").build();

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(clientRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(feedbackRepository.findByMissionIdAndAuthorUserId(1L, client.getId()))
                .thenReturn(Optional.of(new Feedback()));

        CreateFeedbackRequest request = new CreateFeedbackRequest(5, null, null);

        assertThatThrownBy(() -> service.createFeedback(1L, request, user))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Freelancer not assigned to mission cannot leave a feedback")
    void createFeedback_wrongFreelancerForbidden() {
        Freelancer other = new Freelancer();
        other.setId(33L);
        other.setEmail("other@towork.test");
        mission.setAssignedFreelancer(other);

        UserDetails freelancerUser = User.withUsername(freelancer.getEmail())
                .password("pwd").roles("FREELANCER").build();

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(freelancerRepository.findByEmail(freelancer.getEmail())).thenReturn(Optional.of(freelancer));

        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Nice client", FeedbackDirection.FREELANCER_TO_CLIENT);

        assertThatThrownBy(() -> service.createFeedback(1L, request, freelancerUser))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Récupère les feedbacks et agrégats pour un freelance")
    void getFeedbacksForFreelancer_returnsSummary() {
        Feedback feedback = new Feedback();
        feedback.setId(7L);
        feedback.setMission(mission);
        feedback.setAuthorUserId(client.getId());
        feedback.setTargetUserId(freelancer.getId());
        feedback.setDirection(FeedbackDirection.CLIENT_TO_FREELANCER);
        feedback.setRating(5);
        feedback.setComment("Excellent travail");

        when(freelancerRepository.findById(freelancer.getId())).thenReturn(Optional.of(freelancer));
        when(feedbackRepository.findByTargetUserIdAndDirection(freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER))
                .thenReturn(java.util.List.of(feedback));
        when(feedbackRepository.findAverageRatingByTargetUserIdAndDirection(freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER))
                .thenReturn(4.5d);
        when(feedbackRepository.countByTargetUserIdAndDirection(freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER))
                .thenReturn(3L);

        var aggregate = service.getFeedbacksForFreelancer(freelancer.getId());

        assertThat(aggregate.getFeedbacks()).hasSize(1);
        assertThat(aggregate.getSummary().getAverageRating()).isEqualTo(4.5d);
        assertThat(aggregate.getSummary().getCount()).isEqualTo(3L);
    }
}
