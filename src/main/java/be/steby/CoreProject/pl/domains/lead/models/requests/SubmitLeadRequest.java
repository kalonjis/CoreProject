package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for submitting a public inquiry.
 *
 * <p>Used by anonymous visitors to submit inquiries through the
 * public contact form.</p>
 *
 * <p>Validations:</p>
 * <ul>
 *   <li>Email: Required, valid format, max 254 characters</li>
 *   <li>firstName / lastName: Optional, max 100 characters each</li>
 *   <li>Phone: Optional, max 20 characters</li>
 *   <li>OrganisationName: Optional, max 200 characters</li>
 *   <li>Message: Required, 10-5000 characters</li>
 *   <li>Inquiry type: Required</li>
 *   <li>Website: Honeypot field, must be empty</li>
 * </ul>
 *
 * <p>Subject is auto-generated from {@code leadType} — not submitted by the visitor.</p>
 *
 * <p>Usage: POST /api/inquiry</p>
 */
public record SubmitLeadRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        Civility civility,

        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Phone cannot exceed 20 characters")
        String phone,

        @Size(max = 200, message = "Organisation name cannot exceed 200 characters")
        String organisationName,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
        String message,

        @NotNull(message = "Inquiry type is required")
        LeadType leadType,

        LeadSource leadSource,

        // Honeypot field - should always be empty (bots fill this)
        String website

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return LeadRequest for service layer
     */
    public LeadRequest toBllModel() {
        return new LeadRequest(
                email != null ? email.toLowerCase().trim() : null,
                civility,
                firstName != null ? firstName.trim() : null,
                lastName  != null ? lastName.trim()  : null,
                phone     != null ? phone.trim()     : null,
                organisationName != null ? organisationName.trim() : null,
                subjectFromLeadType(leadType),
                message.trim(),
                leadType,
                leadSource,
                website
        );
    }

    private static String subjectFromLeadType(LeadType type) {
        return switch (type) {
            case GENERAL     -> "Question générale";
            case COMMERCIAL  -> "Demande commerciale";
            case PARTNERSHIP -> "Proposition de partenariat";
            case PRESS       -> "Contact presse";
            case OTHER       -> "Autre demande";
        };
    }
}