package be.steby.CoreProject.pl.domains.commercialaction.models.requests;

import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionUpdateRequest;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionType;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PL request model for partially updating an existing commercial action.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * <h3>Reassignment via update</h3>
 * <p>Providing a non-null {@code assignedToPublicId} triggers a reassignment inline
 * with the update and publishes both a {@code CommercialActionReassignedEvent} and
 * a {@code CommercialActionUpdatedEvent}.</p>
 *
 * @param title              new title for the action (optional)
 * @param description        updated description or context (optional)
 * @param priority           new priority level (optional)
 * @param dueDate            new deadline (optional)
 * @param assignedToPublicId public UUID of the new assignee — triggers reassignment if non-null (optional)
 * @param reminderAt         new reminder timestamp — null leaves existing value unchanged (optional)
 * @param location           new free-text location (optional)
 * @param addressPublicId    new physical address public UUID (optional)
 * @param durationMinutes    new meeting duration in minutes (optional)
 */
public record UpdateCommercialActionRequest(

        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        String description,

        CommercialActionType type,

        CommercialActionPriority priority,

        Instant dueDate,

        String assignedToPublicId,

        Instant reminderAt,

        String location,

        String addressPublicId,

        Integer durationMinutes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link CommercialActionUpdateRequest} for the service layer
     */
    public CommercialActionUpdateRequest toBllModel() {
        return new CommercialActionUpdateRequest(
                title       != null ? title.trim()       : null,
                description != null ? description.trim() : null,
                type,
                priority,
                dueDate,
                assignedToPublicId,
                reminderAt,
                location,
                addressPublicId,
                durationMinutes
        );
    }
}
