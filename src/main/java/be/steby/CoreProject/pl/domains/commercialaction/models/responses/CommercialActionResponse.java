package be.steby.CoreProject.pl.domains.commercialaction.models.responses;

import be.steby.CoreProject.dl.entities.crm.CommercialAction;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionStatus;
import be.steby.CoreProject.dl.enums.crm.CommercialActionType;

import java.time.Instant;

/**
 * Response model for a CRM commercial action.
 *
 * <p>Represents the full detail of a commercial action, including its lifecycle status,
 * overdue flag, linked deal/contact, and assigned commercial.
 * Used for both single-record retrieval and list endpoints.</p>
 *
 * @param publicId           public UUID of the commercial action
 * @param title              short descriptive title
 * @param description        optional longer description, or {@code null} if not set
 * @param priority           priority level of the action
 * @param status             current lifecycle status
 * @param dueDate            deadline, or {@code null} if not set
 * @param completedAt        timestamp when the action was completed, or {@code null} if not done
 * @param isOverdue          {@code true} if the action is pending and past its due date
 * @param assignedToPublicId public UUID of the assigned commercial
 * @param assignedToUsername username of the assigned commercial
 * @param dealPublicId       public UUID of the linked deal, or {@code null}
 * @param contactPublicId    public UUID of the linked contact, or {@code null}
 * @param leadPublicId       public UUID of the linked lead, or {@code null}
 * @param reminderAt         scheduled reminder timestamp, or {@code null} if not set
 * @param reminderSentAt     timestamp when the reminder was dispatched, or {@code null} if not yet sent
 * @param location           free-text location (visio link, room…), or {@code null} if not set
 * @param durationMinutes    meeting duration in minutes, or {@code null} if not set
 * @param createdAt          timestamp of entity creation
 * @param updatedAt          timestamp of last update
 */
public record CommercialActionResponse(
        String publicId,
        String title,
        String description,
        CommercialActionType type,
        CommercialActionPriority priority,
        CommercialActionStatus status,
        Instant dueDate,
        Instant completedAt,
        boolean isOverdue,
        String assignedToPublicId,
        String assignedToUsername,
        String dealPublicId,
        String contactPublicId,
        String leadPublicId,
        Instant reminderAt,
        Instant reminderSentAt,
        String location,
        Integer durationMinutes,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a {@link CommercialAction} entity to a {@link CommercialActionResponse}.
     *
     * @param action the commercial action entity
     * @return the response model
     */
    public static CommercialActionResponse fromEntity(CommercialAction action) {
        return new CommercialActionResponse(
                action.getPublicId(),
                action.getTitle(),
                action.getDescription(),
                action.getType(),
                action.getPriority(),
                action.getStatus(),
                action.getDueDate(),
                action.getCompletedAt(),
                action.isOverdue(),
                action.getAssignedTo() != null ? action.getAssignedTo().getPublicId() : null,
                action.getAssignedTo() != null ? action.getAssignedTo().getUsername() : null,
                action.getDeal()       != null ? action.getDeal().getPublicId()       : null,
                action.getContact()    != null ? action.getContact().getPublicId()    : null,
                action.getLead()       != null ? action.getLead().getPublicId()       : null,
                action.getReminderAt(),
                action.getReminderSentAt(),
                action.getLocation(),
                action.getDurationMinutes(),
                action.getCreatedAt(),
                action.getUpdatedAt()
        );
    }
}
