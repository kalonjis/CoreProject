package be.steby.CoreProject.bll.domains.commercialaction.models;

import be.steby.CoreProject.dl.enums.crm.CommercialActionPriority;
import be.steby.CoreProject.dl.enums.crm.CommercialActionType;

import java.time.Instant;

/**
 * BLL request model for creating a new {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}.
 *
 * <p>Used when a commercial creates a task manually against a deal and/or contact.
 * References are provided by their public UUIDs — the service layer resolves them
 * to entity references before persisting.</p>
 *
 * <h3>Validation</h3>
 * <p>At least one of {@code dealPublicId} or {@code contactPublicId} must be set.
 * Enforced at the service layer.</p>
 *
 * @param title                short descriptive title of the action (required)
 * @param description          optional longer description or context (optional)
 * @param type                 type of work to be done; defaults to {@code TASK} if null (optional)
 * @param priority             priority level; defaults to {@code MEDIUM} in the service if null (optional)
 * @param dueDate              deadline for the action (optional)
 * @param assignedToPublicId   public UUID of the commercial responsible for the action (required)
 * @param dealPublicId         public UUID of the linked deal (optional — at least one of lead/deal/contact required)
 * @param contactPublicId      public UUID of the linked contact (optional — at least one of lead/deal/contact required)
 * @param leadPublicId         public UUID of the linked lead (optional — at least one of lead/deal/contact required)
 * @param reminderAt           when to send the reminder notification (optional)
 * @param location             free-text location (visio link, room name…) — relevant for MEETING/DEMO (optional)
 * @param addressPublicId      public UUID of an existing {@code Address} entity — for physical locations (optional)
 * @param durationMinutes      duration in minutes used to compute calendar event end time — defaults to 60 (optional)
 */
public record CommercialActionCreateRequest(
        String title,
        String description,
        CommercialActionType type,
        CommercialActionPriority priority,
        Instant dueDate,
        String assignedToPublicId,
        String dealPublicId,
        String contactPublicId,
        String leadPublicId,
        Instant reminderAt,
        String location,
        String addressPublicId,
        Integer durationMinutes
) {}
