package com.towork.milestone.entity;
import com.towork.user.entity.Client;



public enum MilestoneStatus {
    /**
     * Milestone created but not yet started by the freelancer.
     */
    DRAFT,

    /**
     * Work is underway on the freelancer side.
     */
    IN_PROGRESS,

    /**
     * Deliverable uploaded and waiting for client validation.
     */
    SUBMITTED,

    /**
     * Client rejected the submitted deliverable (freelancer can resubmit).
     */
    REJECTED,

    /**
     * Client accepted the deliverable (ready for payment).
     */
    COMPLETED,

    /**
     * Milestone funds transferred to the freelancer.
     */
    PAID
}
