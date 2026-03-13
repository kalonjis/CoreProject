package be.steby.CoreProject.pl.domains.organisation.models.requests;

import be.steby.CoreProject.bll.domains.organisation.models.OrganisationMergeRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * PL request model for merging two duplicate organisations.
 *
 * <p>The target organisation is kept as the surviving record.
 * The source organisation is removed after all its contacts are reassigned.</p>
 *
 * @param sourcePublicId public UUID of the organisation to be merged and removed
 * @param targetPublicId public UUID of the organisation to keep as the surviving record
 */
public record MergeOrganisationRequest(

        @NotBlank(message = "Source organisation public ID is required")
        String sourcePublicId,

        @NotBlank(message = "Target organisation public ID is required")
        String targetPublicId

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link OrganisationMergeRequest} for the service layer
     */
    public OrganisationMergeRequest toBllModel() {
        return new OrganisationMergeRequest(sourcePublicId, targetPublicId);
    }
}
