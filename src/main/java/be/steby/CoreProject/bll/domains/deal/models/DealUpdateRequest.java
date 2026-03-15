package be.steby.CoreProject.bll.domains.deal.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BLL request model for partially updating an existing {@link be.steby.CoreProject.dl.entities.crm.Deal}.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * <h3>Fields excluded from update</h3>
 * <ul>
 *   <li>{@code status} — managed by {@code DealService.moveToStage} lifecycle operations</li>
 *   <li>{@code pipeline} / {@code pipelineStep} — managed by {@code DealService.moveToStage}</li>
 *   <li>{@code assignedTo} — managed by {@code DealService.reassign}</li>
 *   <li>{@code contact} — immutable after creation</li>
 *   <li>{@code closedAt} — set automatically on terminal stage entry</li>
 * </ul>
 *
 * @param title             new title for the deal (optional)
 * @param amount            new estimated contract value (optional)
 * @param currency          new ISO 4217 currency code (optional)
 * @param expectedCloseDate new expected closing date (optional)
 * @param notes             updated internal notes (optional)
 */
public record DealUpdateRequest(
        String title,
        BigDecimal amount,
        String currency,
        LocalDate expectedCloseDate,
        String notes
) {}
