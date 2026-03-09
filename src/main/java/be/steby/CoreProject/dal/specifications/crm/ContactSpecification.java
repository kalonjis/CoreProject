package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic filtering of {@link Contact} entities.
 *
 * <p>Each static method returns a {@link Specification} that can be combined
 * with others to build complex admin list filters at runtime.</p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * Specification<Contact> spec = Specification
 *     .where(ContactSpecification.hasStatus(ContactStatus.QUALIFIED))
 *     .and(ContactSpecification.belongsToOrganisation(orgId))
 *     .and(ContactSpecification.nameOrEmailContains("thomas"));
 *
 * Page<Contact> results = contactRepository.findAll(spec, pageable);
 * }</pre>
 *
 * <h3>Null safety</h3>
 * <p>All methods return {@code null} when the filter value is {@code null},
 * which JPA treats as "no restriction" — safe to chain unconditionally.</p>
 */
public class ContactSpecification {

    private ContactSpecification() {
        // Utility class — not instantiable
    }

    // =========================================================================
    // Text search
    // =========================================================================

    /**
     * Filters contacts whose first name, last name, or email contains
     * the given keyword (case-insensitive).
     *
     * <p>Used for the search bar in the admin contact list.</p>
     *
     * @param keyword the search term, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Contact> nameOrEmailContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")),  pattern),
                cb.like(cb.lower(root.get("email")),     pattern)
            );
        };
    }

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Filters contacts by their CRM lifecycle status.
     *
     * @param status the status to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Contact> hasStatus(ContactStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // =========================================================================
    // Organisation
    // =========================================================================

    /**
     * Filters contacts belonging to a specific organisation.
     *
     * @param organisationId the internal ID of the organisation, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Contact> belongsToOrganisation(Long organisationId) {
        if (organisationId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("organisation").get("id"), organisationId);
    }

    /**
     * Filters contacts that have no organisation linked.
     *
     * <p>Used to identify independent contacts (sole traders, freelancers).</p>
     *
     * @return the specification
     */
    public static Specification<Contact> withoutOrganisation() {
        return (root, query, cb) -> cb.isNull(root.get("organisation"));
    }

    // =========================================================================
    // User link
    // =========================================================================

    /**
     * Filters contacts that have been linked to a platform account.
     *
     * <p>Used to distinguish signed clients (who have an account)
     * from prospects (who do not yet).</p>
     *
     * @param hasLinkedUser {@code true} to return contacts with a linked user,
     *                      {@code false} for those without, {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Contact> hasLinkedUser(Boolean hasLinkedUser) {
        if (hasLinkedUser == null) return null;
        return (root, query, cb) -> hasLinkedUser
            ? cb.isNotNull(root.get("linkedUser"))
            : cb.isNull(root.get("linkedUser"));
    }

    // =========================================================================
    // Lead conversion
    // =========================================================================

    /**
     * Filters contacts that were converted from a lead.
     *
     * <p>Used for conversion rate reporting.</p>
     *
     * @param convertedFromLead {@code true} to return converted contacts,
     *                          {@code false} for manually created ones,
     *                          {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<Contact> convertedFromLead(Boolean convertedFromLead) {
        if (convertedFromLead == null) return null;
        return (root, query, cb) -> convertedFromLead
            ? cb.isNotNull(root.get("originLead"))
            : cb.isNull(root.get("originLead"));
    }
}