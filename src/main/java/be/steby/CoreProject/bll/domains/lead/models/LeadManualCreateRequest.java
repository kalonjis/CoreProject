package be.steby.CoreProject.bll.domains.lead.models;

import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;

/**
 * Business layer model for manual lead creation by a commercial.
 *
 * <p>Unlike {@link LeadRequest} (anonymous public submission), this model
 * skips honeypot and rate-limit checks. The source is always {@code MANUAL}.</p>
 *
 * @param email            email address of the prospect (required)
 * @param civility         salutation (optional)
 * @param firstName        first name (optional)
 * @param lastName         last name (optional)
 * @param phone            phone number (optional)
 * @param organisationName organisation name (optional)
 * @param subject          subject of the inquiry (required)
 * @param message          initial notes (optional)
 * @param leadType         type of inquiry (required)
 */
public record LeadManualCreateRequest(
        String email,
        Civility civility,
        String firstName,
        String lastName,
        String phone,
        String organisationName,
        String subject,
        String message,
        LeadType leadType
) {}
