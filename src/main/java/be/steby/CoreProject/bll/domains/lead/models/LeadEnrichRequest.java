package be.steby.CoreProject.bll.domains.lead.models;

import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;

/**
 * Request to enrich a lead with contact details collected or completed by the commercial.
 *
 * <p>All fields are optional. A {@code null} value means "do not update this field".
 * An empty string clears the existing value (except enum fields).</p>
 *
 * @param civility         salutation of the prospect
 * @param firstName        first name of the prospect
 * @param lastName         last name of the prospect
 * @param phone            phone number of the prospect
 * @param organisationName name of the organisation the prospect represents
 * @param leadType         corrected inquiry type (if the visitor mis-classified their request)
 * @param leadSource       corrected acquisition source
 */
public record LeadEnrichRequest(
        Civility civility,
        String firstName,
        String lastName,
        String phone,
        String organisationName,
        LeadType leadType,
        LeadSource leadSource
) {}
