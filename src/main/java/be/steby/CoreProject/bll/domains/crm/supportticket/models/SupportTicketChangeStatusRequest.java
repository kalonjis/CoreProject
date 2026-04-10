package be.steby.CoreProject.bll.domains.crm.supportticket.models;

import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

/**
 * BLL request model for changing a support ticket's lifecycle status.
 *
 * @param status the target status (required)
 */
public record SupportTicketChangeStatusRequest(
        SupportTicketStatus status
) {}
