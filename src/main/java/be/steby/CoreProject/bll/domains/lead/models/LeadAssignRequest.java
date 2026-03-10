package be.steby.CoreProject.bll.domains.lead.models;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for assigning or reassigning a lead to a commercial.
 *
 * <p>Used both for initial assignment and reassignment.
 * The distinction is handled at the service layer based on
 * whether the lead already has an {@code assignedTo} value.</p>
 *
 * @param commercialPublicId the public UUID of the commercial to assign the lead to
 */
public record LeadAssignRequest(

        @NotBlank(message = "Commercial public ID is required")
        String commercialPublicId

) {}