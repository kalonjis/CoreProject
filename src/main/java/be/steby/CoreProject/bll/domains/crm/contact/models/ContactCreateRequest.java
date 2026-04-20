package be.steby.CoreProject.bll.domains.crm.contact.models;

/**
 * BLL request model for manually creating a {@link be.steby.CoreProject.dl.entities.crm.Contact}.
 *
 * <p>Used when a commercial creates a contact directly, without going through
 * the lead conversion flow. Status defaults to {@code NEW} at the service layer.</p>
 *
 * <h3>Assignment</h3>
 * <p>Assignment to a commercial is intentionally excluded from this request.
 * It is a separate operation handled by {@link ContactAssignRequest}.</p>
 *
 * <h3>Organisation</h3>
 * <p>{@code organisationPublicId} is optional — a contact may be independent
 * (e.g., sole trader, freelancer).</p>
 *
 * @param firstName            first name of the contact (required)
 * @param lastName             last name of the contact (required)
 * @param email                professional email address (required)
 * @param phone                direct phone number in international format (optional)
 * @param jobTitle             job title or position (optional)
 * @param organisationPublicId public UUID of the organisation to link (optional)
 * @param notes                internal notes for the commercial team (optional)
 */
public record ContactCreateRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String jobTitle,
        String organisationPublicId,
        String notes
) {}