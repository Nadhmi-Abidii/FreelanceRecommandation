package com.towork.user.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ConflictException;
import com.towork.exception.ForbiddenActionException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.dto.CreateFeedbackRequest;
import com.towork.user.dto.FeedbackResponse;
import com.towork.user.dto.FeedbackSummaryResponse;
import com.towork.user.dto.FreelancerFeedbackAggregateResponse;
import com.towork.user.entity.Client;
import com.towork.user.entity.Feedback;
import com.towork.user.entity.FeedbackDirection;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FeedbackRepository;
import com.towork.user.repository.FreelancerRepository;
import com.towork.user.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MissionRepository missionRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;

    @Override
    public FeedbackResponse createFeedback(Long missionId, CreateFeedbackRequest request, UserDetails currentUser) {
        UserDetails user = requireUser(currentUser);
        Mission mission = findMission(missionId);

        if (mission.getStatus() != MissionStatus.COMPLETED) {
            throw new BusinessException("Feedback allowed only when mission is completed");
        }
        if (mission.getAssignedFreelancer() == null) {
            throw new BusinessException("Mission has no assigned freelancer");
        }

        FeedbackDirection direction = resolveDirection(request.getDirection(), user);
        Participant author = resolveAuthor(user);
        Participant missionClient = Participant.client(mission.getClient());
        Participant missionFreelancer = Participant.freelancer(mission.getAssignedFreelancer());

        if (direction == FeedbackDirection.CLIENT_TO_FREELANCER) {
            // Case 1: mission client -> assigned freelancer
            if (!author.isClient() || !Objects.equals(author.id(), missionClient.id())) {
                throw new ForbiddenActionException("Only the mission client can rate the freelancer");
            }
        } else {
            // Case 2: assigned freelancer -> mission client
            if (!author.isFreelancer() || !Objects.equals(author.id(), missionFreelancer.id())) {
                throw new ForbiddenActionException("Only the assigned freelancer can rate the client");
            }
        }

        feedbackRepository.findByMissionIdAndAuthorUserId(missionId, author.id())
                .ifPresent(existing -> { throw new ConflictException("Feedback already exists for this mission"); });

        Feedback feedback = new Feedback();
        feedback.setMission(mission);
        feedback.setAuthorUserId(author.id());
        feedback.setTargetUserId(direction == FeedbackDirection.CLIENT_TO_FREELANCER ? missionFreelancer.id() : missionClient.id());
        feedback.setDirection(direction);
        feedback.setRating(request.getRating());
        feedback.setComment(trimComment(request.getComment()));

        Feedback saved = feedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Override
    public FeedbackResponse getMyFeedback(Long missionId, UserDetails currentUser) {
        UserDetails user = requireUser(currentUser);
        Mission mission = findMission(missionId);
        Participant requester = resolveAuthor(user);
        Participant missionClient = Participant.client(mission.getClient());
        Participant missionFreelancer = Participant.freelancer(mission.getAssignedFreelancer());

        if (!requester.isAdmin() && !Objects.equals(requester.id(), missionClient.id()) && !Objects.equals(requester.id(), missionFreelancer.id())) {
            throw new ForbiddenActionException("You are not part of this mission");
        }

        Long authorId = requester.id();
        Feedback feedback = feedbackRepository.findByMissionIdAndAuthorUserId(missionId, authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for current user"));

        ensureReadAccess(feedback, requester);
        return toResponse(feedback);
    }

    @Override
    public List<FeedbackResponse> getFeedbacksForMission(Long missionId, UserDetails currentUser) {
        UserDetails user = requireUser(currentUser);
        Mission mission = findMission(missionId);
        Participant requester = resolveAuthor(user);
        Participant missionClient = Participant.client(mission.getClient());
        Participant missionFreelancer = Participant.freelancer(mission.getAssignedFreelancer());

        if (!requester.isAdmin()
                && !Objects.equals(requester.id(), missionClient.id())
                && !Objects.equals(requester.id(), missionFreelancer.id())) {
            throw new ForbiddenActionException("Only mission participants can view feedback");
        }

        return feedbackRepository.findByMissionId(missionId).stream()
                .peek(f -> ensureReadAccess(f, requester))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FeedbackSummaryResponse getFeedbackSummary(Long userId) {
        Double average = feedbackRepository.findAverageRatingByTargetUserId(userId);
        Long count = feedbackRepository.countByTargetUserId(userId);
        double avg = average != null ? average : 0.0d;
        long total = count != null ? count : 0L;
        return new FeedbackSummaryResponse(avg, total);
    }

    @Override
    public FreelancerFeedbackAggregateResponse getFeedbacksForFreelancer(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));

        List<FeedbackResponse> feedbacks = feedbackRepository
                .findByTargetUserIdAndDirection(freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER)
                .stream()
                .map(this::toResponse)
                .toList();

        Double average = feedbackRepository.findAverageRatingByTargetUserIdAndDirection(
                freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER);
        Long count = feedbackRepository.countByTargetUserIdAndDirection(
                freelancer.getId(), FeedbackDirection.CLIENT_TO_FREELANCER);
        FeedbackSummaryResponse summary = new FeedbackSummaryResponse(
                average != null ? average : 0.0d,
                count != null ? count : 0L
        );

        return new FreelancerFeedbackAggregateResponse(summary, feedbacks);
    }

    private Mission findMission(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
    }

    private UserDetails requireUser(UserDetails currentUser) {
        if (currentUser == null) {
            throw new ForbiddenActionException("Authentication required");
        }
        return currentUser;
    }

    private Participant resolveAuthor(UserDetails currentUser) {
        if (hasRole(currentUser, "ROLE_ADMIN")) {
            return Participant.admin();
        }
        if (hasRole(currentUser, "ROLE_CLIENT")) {
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new ForbiddenActionException("Client not found"));
            return Participant.client(client);
        }
        if (hasRole(currentUser, "ROLE_FREELANCER")) {
            Freelancer freelancer = freelancerRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new ForbiddenActionException("Freelancer not found"));
            return Participant.freelancer(freelancer);
        }
        throw new ForbiddenActionException("Unsupported role");
    }

    private FeedbackDirection resolveDirection(FeedbackDirection requested, UserDetails user) {
        if (requested != null) {
            return requested;
        }
        if (hasRole(user, "ROLE_CLIENT")) {
            return FeedbackDirection.CLIENT_TO_FREELANCER;
        }
        if (hasRole(user, "ROLE_FREELANCER")) {
            return FeedbackDirection.FREELANCER_TO_CLIENT;
        }
        throw new ForbiddenActionException("Direction is required");
    }

    private void ensureReadAccess(Feedback feedback, Participant requester) {
        if (requester.isAdmin()) {
            return;
        }
        if (!Objects.equals(feedback.getAuthorUserId(), requester.id())
                && !Objects.equals(feedback.getTargetUserId(), requester.id())) {
            throw new ForbiddenActionException("You cannot access this feedback");
        }
    }

    private boolean hasRole(UserDetails user, String role) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals(role));
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getMission() != null ? feedback.getMission().getId() : null,
                feedback.getAuthorUserId(),
                feedback.getTargetUserId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getDirection()
        );
    }

    private String trimComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Participant(Long id, String role) {
        static Participant client(Client client) {
            if (client == null) {
                return new Participant(null, "ROLE_CLIENT");
            }
            return new Participant(client.getId(), "ROLE_CLIENT");
        }

        static Participant freelancer(Freelancer freelancer) {
            if (freelancer == null) {
                return new Participant(null, "ROLE_FREELANCER");
            }
            return new Participant(freelancer.getId(), "ROLE_FREELANCER");
        }

        static Participant admin() {
            return new Participant(null, "ROLE_ADMIN");
        }

        boolean isAdmin() {
            return "ROLE_ADMIN".equals(role);
        }

        boolean isClient() {
            return "ROLE_CLIENT".equals(role);
        }

        boolean isFreelancer() {
            return "ROLE_FREELANCER".equals(role);
        }
    }
}
