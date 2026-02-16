package com.towork.mission.mapper;

import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;
import com.towork.user.entity.Freelancer;
import com.towork.mission.dto.MissionResponse;
import com.towork.mission.entity.Mission;

public final class MissionMapper {
    private MissionMapper() {}

    public static MissionResponse toDto(Mission m) {
        if (m == null) return null;

        Client client = m.getClient();
        Domaine domaine = m.getDomaine();
        Freelancer assigned = m.getAssignedFreelancer();

        Long clientId = client != null ? client.getId() : null;
        Long domaineId = domaine != null ? domaine.getId() : null;
        Long assignedFreelancerId = assigned != null ? assigned.getId() : null;

        String clientCompanyName = client != null ? clean(client.getCompanyName()) : null;
        String clientName = client != null ? resolveClientName(client, clientCompanyName) : null;
        String clientCity = client != null ? clean(client.getCity()) : null;
        String clientCountry = client != null ? clean(client.getCountry()) : null;
        String clientAvatar = client != null ? client.getProfilePicture() : null;
        String domaineName = domaine != null ? clean(domaine.getName()) : null;
        String assignedName = assigned != null ? joinNames(assigned.getFirstName(), assigned.getLastName()) : null;
        String assignedEmail = assigned != null ? assigned.getEmail() : null;

        return new MissionResponse(
            m.getId(),
            clientId,
            domaineId,
            assignedFreelancerId,
            clientName,
            clientCompanyName,
            clientCity,
            clientCountry,
            clientAvatar,
            domaineName,
            assignedName,
            assignedEmail,
            m.getTitle(),
            m.getDescription(),
            m.getRequirements(),
            m.getBudgetMin(),
            m.getBudgetMax(),
            m.getBudgetType(),
            m.getTypeTravail(),
            m.getNiveauExperience(),
            m.getStatus(),
            m.getDeadline(),
            m.getEstimatedDuration(),
            m.getSkillsRequired(),
            m.getIsUrgent(),
            m.getAttachments(),
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }

    private static String resolveClientName(Client client, String companyName) {
        if (companyName != null) {
            return companyName;
        }
        String fullName = joinNames(client.getFirstName(), client.getLastName());
        if (fullName != null) {
            return fullName;
        }
        return client.getEmail();
    }

    private static String joinNames(String first, String last) {
        StringBuilder builder = new StringBuilder();
        if (first != null && !first.isBlank()) {
            builder.append(first.trim());
        }
        if (last != null && !last.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(last.trim());
        }
        String full = builder.toString().trim();
        return full.isEmpty() ? null : full;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
