package be.steby.CoreProject.pl.domains.contact.models.requests;

import be.steby.CoreProject.bll.domains.crm.contact.models.ContactMergeRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * PL request model for merging two duplicate contacts.
 *
 * <p>The target contact is kept as the surviving record.
 * The source contact is archived (status set to {@code INACTIVE}) after the merge.</p>
 *
 * @param sourcePublicId public UUID of the contact to be merged and archived
 * @param targetPublicId public UUID of the contact to keep as the surviving record
 */
public record MergeContactRequest(

        @NotBlank(message = "Source contact public ID is required")
        String sourcePublicId,

        @NotBlank(message = "Target contact public ID is required")
        String targetPublicId

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link ContactMergeRequest} for the service layer
     */
    public ContactMergeRequest toBllModel() {
        return new ContactMergeRequest(sourcePublicId, targetPublicId);
    }
}
