package com.towork.mission.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;

public interface MissionService {
    Mission createMission(Mission mission);
    Mission updateMission(Long id, Mission mission);
    void deleteMission(Long id);
    Mission getMissionById(Long id);
    List<Mission> getMissionsByClient(Long clientId);
    List<Mission> getMissionsByDomaine(Long domaineId);
    List<Mission> getMissionsByStatus(MissionStatus status);
    List<Mission> getPublishedMissions();
    List<Mission> getUrgentMissions();
    Page<Mission> getAllMissions(Pageable pageable);
    Mission updateMissionStatus(Long id, MissionStatus status);
    List<Mission> searchMissions(String keyword, Long domaineId, TypeTravail typeTravail, NiveauExperience niveauExperience);
    Mission completeMissionForFreelancer(Long missionId, UserDetails currentUser);
    Mission submitFinalDelivery(Long missionId, UserDetails currentUser);
    Mission closeMission(Long missionId, UserDetails currentUser);

}
