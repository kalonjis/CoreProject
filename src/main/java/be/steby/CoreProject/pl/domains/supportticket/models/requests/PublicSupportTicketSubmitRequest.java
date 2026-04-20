package be.steby.CoreProject.pl.domains.supportticket.models.requests;

import be.steby.CoreProject.bll.domains.crm.supportticket.models.PublicSupportTicketRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for a support ticket submitted via the public contact form.
 *
 * <p>firstName and lastName are required (unlike the general inquiry form) so that
 * an auto-created Contact always has a valid full name.</p>
 *
 * <p>Usage: POST /api/public/support</p>
 */
public record PublicSupportTicketSubmitRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        String subject,

        @Size(max = 3000, message = "Description cannot exceed 3000 characters")
        String description,

        // Honeypot field — must be empty
        String website

) {
    public PublicSupportTicketRequest toBllModel() {
        return new PublicSupportTicketRequest(
                firstName.trim(),
                lastName.trim(),
                email.toLowerCase().trim(),
                subject.trim(),
                description != null ? description.trim() : null,
                website
        );
    }
}
