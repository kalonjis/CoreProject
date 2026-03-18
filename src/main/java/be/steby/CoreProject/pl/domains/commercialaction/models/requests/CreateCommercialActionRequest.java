package be.steby.CoreProject.pl.domains.commercialaction.models.requests;

import be.steby.CoreProject.bll.domains.commercialaction.models.CommercialActionCreateRequest;
import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PL request model for creating a new commercial action.
 *
 * <p>At least one of {@code dealPublicId} or {@code contactPublicId} must be set —
 * validated at the service layer.</p>
 *
 * @param title              short descriptive title of the action (required)
 * @param description        optional longer description or context (optional)
 * @param priority           priority level; defaults to {@code MEDIUM} in the service if null (optional)
 * @param dueDate            deadline for the action (optional)
 * @param assignedToPublicId public UUID of the commercial responsible for the action (required)
 * @param dealPublicId       public UUID of the linked deal (optional — at least one of lead/deal/contact required)
 * @param contactPublicId    public UUID of the linked contact (optional — at least one of lead/deal/contact required)
 * @param leadPublicId       public UUID of the linked lead (optional — at least one of lead/deal/contact required)
 * @param reminderAt         when to send the reminder notification (optional)
 */
public record CreateCommercialActionRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        String description,

        CommercialActionPriority priority,

        Instant dueDate,

        @NotBlank(message = "Assigned commercial is required")
        String assignedToPublicId,

        String dealPublicId,

        String contactPublicId,

        String leadPublicId,

        Instant reminderAt

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link CommercialActionCreateRequest} for the service layer
     */
    public CommercialActionCreateRequest toBllModel() {
        return new CommercialActionCreateRequest(
                title.trim(),
                description != null ? description.trim() : null,
                priority,
                dueDate,
                assignedToPublicId,
                dealPublicId,
                contactPublicId,
                leadPublicId,
                reminderAt
        );
    }
}
