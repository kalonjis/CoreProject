package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PL request model for transitioning a contact to a new CRM lifecycle status.
 *
 * <p>Valid transitions are enforced at the service layer:</p>
 * <pre>
 * NEW      → ENGAGED, INACTIVE, LOST
 * ENGAGED  → QUALIFIED, INACTIVE, LOST
 * QUALIFIED→ CLIENT, LOST, INACTIVE
 * CLIENT   → LOST, INACTIVE
 * LOST     → ENGAGED
 * INACTIVE → ENGAGED
 * </pre>
 *
 * @param status the target CRM status (required)
 */
public record UpdateContactStatusRequest(

        @NotNull(message = "Status is required")
        ContactStatus status

) {}
