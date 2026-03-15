package be.steby.CoreProject.bll.domains.supportticket.models;

import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

/**
 * BLL filter model for querying support tickets with optional criteria.
 *
 * <p>All fields are optional — null values are ignored in the specification.</p>
 *
 * @param keyword             keyword to search in subject (optional)
 * @param status              filter by lifecycle status (optional)
 * @param contactPublicId     filter by submitting contact (optional)
 * @param assignedToPublicId  filter by assigned team member (optional)
 * @param unassignedOnly      if {@code true}, returns only unassigned tickets (optional)
 */
public record SupportTicketFilterRequest(
        String keyword,
        SupportTicketStatus status,
        String contactPublicId,
        String assignedToPublicId,
        Boolean unassignedOnly
) {}
