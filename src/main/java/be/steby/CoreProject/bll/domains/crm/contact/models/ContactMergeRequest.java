package be.steby.CoreProject.bll.domains.crm.contact.models;

/**
 * BLL request model for merging two duplicate {@link be.steby.CoreProject.dl.entities.crm.Contact}
 * entries into one.
 *
 * <h3>Merge strategy</h3>
 * <p>The {@code targetPublicId} contact is kept as the surviving record.
 * The {@code sourcePublicId} contact is archived after the merge.</p>
 *
 * <p>The service layer is responsible for:</p>
 * <ul>
 *   <li>Reassigning all {@code Deal} and {@code Interaction} records
 *       from source to target</li>
 *   <li>Preserving {@code originLead} traceability on the target if not already set</li>
 *   <li>Copying any non-null fields from source to target when target fields are blank</li>
 *   <li>Archiving (not deleting) the source contact for audit purposes</li>
 * </ul>
 *
 * <h3>Constraints</h3>
 * <p>Source and target must be different contacts. The service layer
 * enforces this and throws {@code ContactMergeException} if they are identical.</p>
 *
 * @param sourcePublicId public UUID of the contact to be merged and archived
 * @param targetPublicId public UUID of the contact to keep as the surviving record
 */
public record ContactMergeRequest(
        String sourcePublicId,
        String targetPublicId
) {}