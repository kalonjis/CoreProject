package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadConvertRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PL request model for converting a lead into a Contact.
 *
 * @param firstName            first name of the contact to create
 * @param lastName             last name of the contact to create
 * @param email                optional email override — if provided, replaces the lead's email on the contact
 * @param jobTitle             optional job title of the contact
 * @param phone                optional direct phone number of the contact
 * @param organisationPublicId optional public UUID of an existing organisation to link
 * @param organisationName     optional organisation name — used to find or create the organisation
 */
public record ConvertLeadRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        String organisationPublicId,

        @Size(max = 200, message = "Organisation name must not exceed 200 characters")
        String organisationName

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link LeadConvertRequest} for the service layer
     */
    public LeadConvertRequest toBllModel() {
        return new LeadConvertRequest(
                firstName.trim(),
                lastName.trim(),
                email != null ? email.toLowerCase().trim() : null,
                jobTitle != null ? jobTitle.trim() : null,
                phone != null ? phone.trim() : null,
                organisationPublicId,
                organisationName != null ? organisationName.trim() : null
        );
    }
}