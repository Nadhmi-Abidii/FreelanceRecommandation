package com.towork.user.repository;

import com.towork.user.entity.Competence;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetenceRepository extends JpaRepository<Competence, Long> {

    List<Competence> findByFreelancer(Freelancer freelancer);

    List<Competence> findByNameContainingIgnoreCase(String name);

    List<Competence> findByLevel(String level);

    List<Competence> findByIsCertified(Boolean isCertified);

    @Query("SELECT c FROM Competence c WHERE c.freelancer = :freelancer AND c.isActive = true")
    List<Competence> findActiveByFreelancer(@Param("freelancer") Freelancer freelancer);

    @Query("SELECT c FROM Competence c WHERE c.name = :name AND c.freelancer = :freelancer")
    List<Competence> findByNameAndFreelancer(@Param("name") String name, @Param("freelancer") Freelancer freelancer);

    @Query("SELECT DISTINCT c.name FROM Competence c WHERE c.isActive = true")
    List<String> findDistinctCompetenceNames();
}
