package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.dl.enums.AdminDeactivationCategory;

import java.time.LocalDateTime;

public record AdminDeactivationRequest(
        Long targetUserId,
        AdminDeactivationCategory category,
        String detailedReason,
        boolean notifyUser,
        boolean immediateDeactivation,
        LocalDateTime scheduledFor
) {

    /**
     * Constructor pour désactivation immédiate simple
     */
    public AdminDeactivationRequest(Long targetUserId,
                                    AdminDeactivationCategory category,
                                    String detailedReason) {
        this(targetUserId, category, detailedReason, true, true, null);
    }

    /**
     * Constructor pour désactivation immédiate avec contrôle notification
     */
    public AdminDeactivationRequest(Long targetUserId,
                                    AdminDeactivationCategory category,
                                    String detailedReason,
                                    boolean notifyUser) {
        this(targetUserId, category, detailedReason, notifyUser, true, null);
    }
}
