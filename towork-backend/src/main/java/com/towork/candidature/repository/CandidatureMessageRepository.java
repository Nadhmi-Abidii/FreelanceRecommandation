package com.towork.candidature.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;

public interface CandidatureMessageRepository extends JpaRepository<CandidatureMessage, Long> {
    List<CandidatureMessage> findByCandidatureOrderByCreatedAtAsc(Candidature candidature);
}
