package be.steby.CoreProject.bll.domains.crm.call.models;

/**
 * BLL request model for initiating a call from the CRM.
 *
 * <p>For OUTBOUND calls, at least one of {@code contactPublicId} or {@code leadPublicId}
 * must be non-null. This constraint is enforced by {@code CallServiceImpl}.</p>
 * <p>For INBOUND calls the contact/lead may be unknown at answer time — the constraint
 * is skipped when {@code direction} is {@code "INBOUND"}.</p>
 *
 * @param phoneNumber      the number to dial / caller number
 * @param contactPublicId  public UUID of the contact; {@code null} for lead or inbound
 * @param leadPublicId     public UUID of the lead; {@code null} for contact or inbound
 * @param direction        {@code "INBOUND"} or {@code "OUTBOUND"} (default)
 */
public record InitiateCallRequest(
        String phoneNumber,
        String contactPublicId,
        String leadPublicId,
        String direction
) {}
