package com.towork.mission.service;

import com.towork.mission.dto.FreelancerMissionWithMilestonesDto;

import java.util.List;

public interface FreelancerMissionService {
    List<FreelancerMissionWithMilestonesDto> getMissionsForFreelancer(String freelancerEmail);
}
