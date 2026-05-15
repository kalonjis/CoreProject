package be.steby.CoreProject.bll.domains.crm.call.models;

/**
 * BLL request model for initiating a call from the CRM.
 *
 * <p>At least one of {@code contactPublicId} or {@code leadPublicId} must be non-null.
 * This constraint is enforced by {@code CallServiceImpl} before delegating
 * to the telephony adapter.</p>
 *
 * @param phoneNumber      the number to dial
 * @param contactPublicId  public UUID of the contact being called; {@code null} if calling a lead
 * @param leadPublicId     public UUID of the lead being called; {@code null} if calling a contact
 */
public record InitiateCallRequest(
        String phoneNumber,
        String contactPublicId,
        String leadPublicId
) {}
