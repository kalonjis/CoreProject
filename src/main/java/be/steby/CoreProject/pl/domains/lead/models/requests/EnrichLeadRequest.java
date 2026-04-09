package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadEnrichRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for enriching a lead with commercial-collected contact details.
 *
 * <p>All fields are optional. A {@code null} value means "do not update".
 * An empty string clears the existing value (except enum fields).</p>
 *
 * @param civility         salutation of the prospect
 * @param firstName        first name of the prospect
 * @param lastName         last name of the prospect
 * @param phone            phone number of the prospect
 * @param organisationName name of the organisation the prospect represents
 * @param leadType         corrected inquiry type
 * @param leadSource       corrected acquisition source
 */
public record EnrichLeadRequest(
        Civility civility,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20)  String phone,
        @Size(max = 200) String organisationName,
        LeadType leadType,
        LeadSource leadSource
) {
    public LeadEnrichRequest toBllModel() {
        return new LeadEnrichRequest(civility, firstName, lastName, phone, organisationName, leadType, leadSource);
    }
}
