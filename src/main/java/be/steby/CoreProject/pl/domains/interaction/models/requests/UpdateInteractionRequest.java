package be.steby.CoreProject.pl.domains.interaction.models.requests;

import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionUpdateRequest;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PL request model for partially updating an existing CRM interaction.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * <h3>Fields excluded from update</h3>
 * <ul>
 *   <li>{@code type}            — immutable after creation</li>
 *   <li>{@code direction}       — immutable after creation</li>
 *   <li>{@code deal}            — immutable after creation</li>
 *   <li>{@code contact}         — immutable after creation</li>
 *   <li>{@code performedBy}     — immutable after creation</li>
 *   <li>{@code callLog}         — managed via dedicated call log endpoints</li>
 *   <li>{@code emailLog}        — managed via dedicated email log endpoints</li>
 * </ul>
 *
 * @param subject          new subject or title for the interaction (optional)
 * @param notes            updated free-text notes (optional)
 * @param outcome          new outcome classification (optional)
 * @param durationMinutes  new duration in minutes (optional)
 * @param occurredAt       corrected occurrence timestamp (optional)
 */
public record UpdateInteractionRequest(

        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        String notes,

        InteractionOutcome outcome,

        Integer durationMinutes,

        Instant occurredAt

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link InteractionUpdateRequest} for the service layer
     */
    public InteractionUpdateRequest toBllModel() {
        return new InteractionUpdateRequest(
                subject != null ? subject.trim() : null,
                notes,
                outcome,
                durationMinutes,
                occurredAt
        );
    }
}
