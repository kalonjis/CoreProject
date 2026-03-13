package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.contact.models.ContactUpdateRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * PL request model for partially updating an existing contact.
 *
 * <p>All fields are optional — only non-null fields are applied at the service layer.</p>
 *
 * @param firstName first name of the contact (optional)
 * @param lastName  last name of the contact (optional)
 * @param email     professional email address (optional)
 * @param phone     direct phone number in international format (optional)
 * @param jobTitle  job title or position (optional)
 * @param notes     internal notes for the commercial team (optional)
 */
public record UpdateContactRequest(

        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        String notes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link ContactUpdateRequest} for the service layer
     */
    public ContactUpdateRequest toBllModel() {
        return new ContactUpdateRequest(
                firstName != null ? firstName.trim() : null,
                lastName  != null ? lastName.trim()  : null,
                email     != null ? email.toLowerCase().trim() : null,
                phone     != null ? phone.trim()     : null,
                jobTitle  != null ? jobTitle.trim()  : null,
                notes     != null ? notes.trim()     : null
        );
    }
}
