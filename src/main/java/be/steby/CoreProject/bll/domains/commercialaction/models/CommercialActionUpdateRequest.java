package be.steby.CoreProject.bll.domains.commercialaction.models;

import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;

import java.time.Instant;

/**
 * BLL request model for partially updating an existing {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * <h3>Fields excluded from update</h3>
 * <ul>
 *   <li>{@code status} — managed by {@code CommercialActionService.complete} and {@code .cancel}</li>
 *   <li>{@code completedAt} — set automatically on status transition to {@code DONE}</li>
 *   <li>{@code deal} / {@code contact} — immutable after creation</li>
 * </ul>
 *
 * <h3>Reassignment via update</h3>
 * <p>If {@code assignedToPublicId} is non-null, the service will reassign the action
 * and publish a {@code CommercialActionReassignedEvent} in addition to the update event.</p>
 *
 * @param title              new title for the action (optional)
 * @param description        updated description or context (optional)
 * @param priority           new priority level (optional)
 * @param dueDate            new deadline (optional)
 * @param assignedToPublicId public UUID of the new assignee — triggers reassignment if non-null (optional)
 */
public record CommercialActionUpdateRequest(
        String title,
        String description,
        CommercialActionPriority priority,
        Instant dueDate,
        String assignedToPublicId
) {}
