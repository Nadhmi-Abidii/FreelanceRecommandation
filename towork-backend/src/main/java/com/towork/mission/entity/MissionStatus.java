package com.towork.mission.entity;

public enum MissionStatus {
    DRAFT,
    PUBLISHED,
    IN_PROGRESS,
    /**
     * The freelancer submitted the final delivery and the mission waits for client closure/payment.
     */
    PENDING_CLOSURE,
    COMPLETED,
    CANCELLED,
    PAUSED
}
