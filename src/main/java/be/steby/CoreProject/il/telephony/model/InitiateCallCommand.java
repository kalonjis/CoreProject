package be.steby.CoreProject.il.telephony.model;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Command carrying the parameters required to initiate a call.
 *
 * <p>At least one of {@code contact} or {@code lead} must be non-null.
 * This constraint is enforced by the service layer before the command is passed
 * to a {@link be.steby.CoreProject.il.telephony.TelephonyPort} adapter.</p>
 *
 * @param phoneNumber  the number to dial, in any format accepted by the provider
 * @param contact      the CRM contact being called; {@code null} if calling a lead
 * @param lead         the CRM lead being called; {@code null} if calling a contact
 * @param performedBy  the commercial initiating the call
 */
public record InitiateCallCommand(
        String phoneNumber,
        Contact contact,
        Lead lead,
        User performedBy
) {}
