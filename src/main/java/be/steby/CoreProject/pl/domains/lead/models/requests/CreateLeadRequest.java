package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadManualCreateRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.Civility;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request model for manual lead creation by a commercial.
 *
 * <p>Used by authenticated commercials to encode a lead directly from the CRM
 * (phone call, business card, trade show, etc.).
 * Bypasses honeypot and rate-limit checks.</p>
 */
public record CreateLeadRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        Civility civility,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone,

        @Size(max = 200)
        String organisationName,

        @NotBlank(message = "Subject is required")
        @Size(min = 2, max = 255, message = "Subject must be between 2 and 255 characters")
        String subject,

        @Size(max = 5000)
        String message,

        @NotNull(message = "Lead type is required")
        LeadType leadType

) {

    public LeadManualCreateRequest toBllModel() {
        return new LeadManualCreateRequest(
                email.toLowerCase().trim(),
                civility,
                firstName  != null ? firstName.trim()  : null,
                lastName   != null ? lastName.trim()   : null,
                phone      != null ? phone.trim()      : null,
                organisationName != null ? organisationName.trim() : null,
                subject.trim(),
                message    != null ? message.trim()    : null,
                leadType
        );
    }
}
