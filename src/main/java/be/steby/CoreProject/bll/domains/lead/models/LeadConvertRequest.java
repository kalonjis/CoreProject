package be.steby.CoreProject.bll.domains.lead.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for converting a lead into a Contact.
 *
 * <p>A lead only carries a raw {@code name} and {@code email}.
 * At conversion time, the commercial must provide the structured
 * identity fields required to create a proper {@code Contact}.</p>
 *
 * <p>The {@code organisationPublicId} is optional — a contact can be
 * independent (e.g. sole trader) and linked to an organisation later.</p>
 *
 * @param firstName           first name of the contact to create
 * @param lastName            last name of the contact to create
 * @param jobTitle            optional job title of the contact
 * @param phone               optional direct phone number of the contact
 * @param organisationPublicId optional public UUID of an existing organisation to link
 */
public record LeadConvertRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        String organisationPublicId

) {}