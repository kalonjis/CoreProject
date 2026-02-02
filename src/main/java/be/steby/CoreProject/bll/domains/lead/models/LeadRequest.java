package be.steby.CoreProject.bll.domains.lead.models;

import be.steby.CoreProject.dl.enums.LeadType;

/**
 * Business layer model for public inquiry submission.
 *
 * <p>Contains all data from the public contact form submitted by
 * anonymous visitors.</p>
 *
 * @param email       Email address of the visitor (required)
 * @param name        Name of the visitor (optional)
 * @param subject     Subject of the inquiry (required)
 * @param message     Content of the message (required, not persisted in DB)
 * @param leadType Type of inquiry (required)
 * @param website     Honeypot field - must be empty (bots fill this)
 */
public record LeadRequest(
        String email,
        String name,
        String subject,
        String message,
        LeadType leadType,
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

    /**
     * Checks if a name was provided in the request.
     *
     * @return true if name is present and not blank
     */
    public boolean hasName() {
        return name != null && !name.isBlank();
    }
}