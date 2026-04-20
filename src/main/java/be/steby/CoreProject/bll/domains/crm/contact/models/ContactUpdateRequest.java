package be.steby.CoreProject.bll.domains.crm.contact.models;

/**
 * BLL request model for updating an existing {@link be.steby.CoreProject.dl.entities.crm.Contact}.
 *
 * <p>Only editable fields are included. Fields that are managed by dedicated
 * operations are intentionally excluded:</p>
 * <ul>
 *   <li>{@code status} — managed by {@code ContactService} based on deal outcomes</li>
 *   <li>{@code assignedTo} — managed by {@link ContactAssignRequest}</li>
 *   <li>{@code organisation} — managed by {@link ContactLinkOrganisationRequest}</li>
 *   <li>{@code originLead} / {@code linkedUser} — immutable after creation</li>
 * </ul>
 *
 * <h3>Partial update</h3>
 * <p>All fields are optional. The service layer only updates fields
 * that are non-null in this request, leaving the rest unchanged.</p>
 *
 * @param firstName first name of the contact (optional)
 * @param lastName  last name of the contact (optional)
 * @param email     professional email address (optional)
 * @param phone     direct phone number in international format (optional)
 * @param jobTitle  job title or position (optional)
 * @param notes     internal notes for the commercial team (optional)
 */
public record ContactUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String jobTitle,
        String notes
) {}