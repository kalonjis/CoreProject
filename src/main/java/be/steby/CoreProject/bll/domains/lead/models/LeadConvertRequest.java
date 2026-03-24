package be.steby.CoreProject.bll.domains.lead.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for converting a lead into a Contact.
 *
 * <p>A lead only carries a raw {@code name} and {@code email}.
 * At conversion time, the commercial must provide the structured
 * identity fields required to create a proper {@code Contact}.</p>
 *
 * <p>Organisation resolution priority (mutually exclusive):</p>
 * <ol>
 *   <li>{@code organisationPublicId} — links to an existing organisation directly</li>
 *   <li>{@code organisationName} — finds an existing organisation by name (case-insensitive)
 *       or creates a new one on the fly</li>
 * </ol>
 * <p>If neither is provided, the contact is created without an organisation link.</p>
 *
 * @param firstName            first name of the contact to create
 * @param lastName             last name of the contact to create
 * @param email                optional email override — if provided, replaces the lead's email on the contact
 * @param jobTitle             optional job title of the contact
 * @param phone                optional direct phone number of the contact
 * @param organisationPublicId optional public UUID of an existing organisation to link
 * @param organisationName     optional organisation name — used to find or create the organisation
 */
public record LeadConvertRequest(

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

) {}