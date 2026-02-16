package com.towork;

import com.towork.candidature.service.CandidatureService;
import com.towork.mission.controller.MissionController;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.service.MissionService;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class MissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MissionService missionService;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private CandidatureService candidatureService;

    @MockBean
    private FreelancerRepository freelancerRepository;

    @Test
    void submitFinalDelivery_returnsPendingClosureMission() throws Exception {
        Mission mission = new Mission();
        mission.setId(1L);
        mission.setStatus(MissionStatus.PENDING_CLOSURE);

        when(missionService.submitFinalDelivery(eq(1L), any())).thenReturn(mission);

        mockMvc.perform(post("/missions/1/submit-final")
                        .with(SecurityMockMvcRequestPostProcessors.user("freelancer").roles("FREELANCER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value(MissionStatus.PENDING_CLOSURE.name()));
    }

    @Test
    void closeMission_returnsCompletedMission() throws Exception {
        Mission mission = new Mission();
        mission.setId(2L);
        mission.setStatus(MissionStatus.COMPLETED);

        when(missionService.closeMission(eq(2L), any())).thenReturn(mission);

        mockMvc.perform(post("/missions/2/close")
                        .with(SecurityMockMvcRequestPostProcessors.user("client").roles("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mission closed successfully"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.status").value(MissionStatus.COMPLETED.name()));
    }
}
