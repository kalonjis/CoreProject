package be.steby.CoreProject.bll.domains.crm.organisation.models;

/**
 * BLL request model for merging two duplicate {@link be.steby.CoreProject.dl.entities.crm.Organisation}
 * entries into one.
 *
 * <h3>Merge strategy</h3>
 * <p>The {@code targetPublicId} organisation is kept as the surviving record.
 * The {@code sourcePublicId} organisation is archived after the merge.</p>
 *
 * <p>The service layer is responsible for:</p>
 * <ul>
 *   <li>Reassigning all linked {@code Contact} records from source to target</li>
 *   <li>Copying any non-null fields from source to target when target fields are blank</li>
 *   <li>Archiving (not deleting) the source organisation for audit purposes</li>
 * </ul>
 *
 * <h3>Constraints</h3>
 * <p>Source and target must be different organisations. The service layer
 * enforces this and throws {@code OrganisationMergeException} if they are identical.</p>
 *
 * @param sourcePublicId public UUID of the organisation to be merged and archived
 * @param targetPublicId public UUID of the organisation to keep as the surviving record
 */
public record OrganisationMergeRequest(
        String sourcePublicId,
        String targetPublicId
) {}
