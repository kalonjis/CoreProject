package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.contact.models.ContactCreateRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for manually creating a contact.
 *
 * @param firstName            first name of the contact (required)
 * @param lastName             last name of the contact (required)
 * @param email                professional email address (required)
 * @param phone                direct phone number in international format (optional)
 * @param jobTitle             job title or position (optional)
 * @param organisationPublicId public UUID of the organisation to link (optional)
 * @param notes                internal notes for the commercial team (optional)
 */
public record CreateContactRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        String organisationPublicId,

        String notes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link ContactCreateRequest} for the service layer
     */
    public ContactCreateRequest toBllModel() {
        return new ContactCreateRequest(
                firstName.trim(),
                lastName.trim(),
                email.toLowerCase().trim(),
                phone    != null ? phone.trim()    : null,
                jobTitle != null ? jobTitle.trim() : null,
                organisationPublicId,
                notes    != null ? notes.trim()    : null
        );
    }
}
