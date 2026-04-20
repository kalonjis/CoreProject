package be.steby.CoreProject.bll.domains.crm.interaction.models;

import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;

import java.time.Instant;

/**
 * BLL request model for partially updating an existing {@link be.steby.CoreProject.dl.entities.crm.Interaction}.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * <h3>Fields excluded from update</h3>
 * <ul>
 *   <li>{@code type}             — immutable after creation (determines CallLog/EmailLog presence)</li>
 *   <li>{@code direction}        — immutable after creation</li>
 *   <li>{@code deal}             — immutable after creation</li>
 *   <li>{@code contact}          — immutable after creation</li>
 *   <li>{@code performedBy}      — immutable after creation</li>
 *   <li>{@code callLogDetails}   — managed via CallLog entity directly</li>
 *   <li>{@code emailLogDetails}  — managed via EmailLog entity directly</li>
 * </ul>
 *
 * @param subject          new subject or title (optional)
 * @param notes            updated free-text notes (optional)
 * @param outcome          new outcome classification (optional)
 * @param durationMinutes  new duration in minutes (optional)
 * @param occurredAt       corrected occurrence timestamp (optional)
 */
public record InteractionUpdateRequest(
        String subject,
        String notes,
        InteractionOutcome outcome,
        Integer durationMinutes,
        Instant occurredAt
) {}
