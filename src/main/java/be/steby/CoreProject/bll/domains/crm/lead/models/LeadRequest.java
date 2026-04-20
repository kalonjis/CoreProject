package be.steby.CoreProject.bll.domains.crm.lead.models;

import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;

/**
 * Business layer model for public inquiry submission.
 *
 * <p>Contains all data from the public contact form submitted by
 * anonymous visitors.</p>
 *
 * @param email            email address of the visitor (required)
 * @param civility         salutation of the visitor (optional)
 * @param firstName        first name of the visitor (optional)
 * @param lastName         last name of the visitor (optional)
 * @param phone            phone number of the visitor (optional)
 * @param jobTitle         job title of the visitor (optional)
 * @param organisationName name of the organisation the visitor represents (optional)
 * @param subject          subject of the inquiry (required)
 * @param message          content of the message (required, not persisted in DB)
 * @param leadType         type of inquiry (required)
 * @param leadSource       acquisition source detected from UTM params (optional)
 * @param website          honeypot field - must be empty (bots fill this)
 */
public record LeadRequest(
        String email,
        Civility civility,
        String firstName,
        String lastName,
        String phone,
        String jobTitle,
        String organisationName,
        String subject,
        String message,
        LeadType leadType,
        LeadSource leadSource,
        String website
) {

    /**
     * Checks if the honeypot field is filled (indicates bot).
     *
     * @return true if honeypot is filled
     */
    public boolean isHoneypotFilled() {
        return website != null && !website.isBlank();
    }

    /**
     * Checks if an email was provided in the request.
     *
     * @return true if email is present and not blank
     */
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}