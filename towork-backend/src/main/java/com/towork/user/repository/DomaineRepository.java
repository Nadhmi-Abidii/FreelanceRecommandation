package com.towork.user.repository;

import com.towork.user.entity.Domaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomaineRepository extends JpaRepository<Domaine, Long> {

    Optional<Domaine> findByName(String name);

    List<Domaine> findByIsActive(Boolean isActive);

    @Query("SELECT d FROM Domaine d WHERE d.isActive = true ORDER BY d.name ASC")
    List<Domaine> findActiveDomainesOrderByName();

    @Query("SELECT d FROM Domaine d WHERE d.name LIKE %:name% AND d.isActive = true")
    List<Domaine> findByNameContainingIgnoreCase(@Param("name") String name);
}
