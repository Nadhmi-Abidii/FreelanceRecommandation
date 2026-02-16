package com.towork.candidature.service;

import com.towork.mission.entity.Mission;
import com.towork.user.entity.Freelancer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;
import com.towork.candidature.entity.CandidatureMessageAuthor;
import com.towork.candidature.entity.CandidatureStatus;

public interface CandidatureService {
    Candidature createCandidature(Candidature candidature);
    Candidature updateCandidature(Long id, Candidature candidature);
    void deleteCandidature(Long id);
    Candidature getCandidatureById(Long id);
    List<Candidature> getCandidaturesByFreelancer(Freelancer freelancer);
    List<Candidature> getCandidaturesByMission(Mission mission);
    List<Candidature> getCandidaturesByStatus(CandidatureStatus status);
    Page<Candidature> getAllCandidatures(Pageable pageable);
    Candidature updateCandidatureStatus(Long id, CandidatureStatus status, String clientMessage, Long actingClientId);
    boolean hasFreelancerAppliedToMission(Freelancer freelancer, Mission mission);
    CandidatureMessage addMessage(Long candidatureId, CandidatureMessageAuthor author, String content, String resumeUrl);
    List<CandidatureMessage> getMessages(Long candidatureId);
}
