package be.steby.CoreProject.pl.domains.admin.models.responses;

import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;

/**
 * Read-only DTO representing a single administrative deactivation category
 * as exposed by the REST API.
 *
 * <p>This record is a flat projection of {@link AdminDeactivationCategory},
 * designed to be serialized to JSON and consumed by the admin front-end.
 * It decouples the API contract from the internal enum structure, allowing
 * the enum to evolve without breaking API consumers.
 *
 * <p>Returned as a list by:
 * <pre>GET /api/admin/users/deactivation-categories</pre>
 *
 * <h3>Usage</h3>
 * The front-end uses this list to populate the category picker in the
 * deactivation modal. The {@code requiresSuperAdmin} flag is used to
 * hide sensitive categories from regular {@code ADMIN} users, while
 * {@code allowsReactivation} drives the reactivation eligibility hint
 * shown alongside each option.
 *
 * <h3>Example JSON</h3>
 * <pre>{@code
 * {
 *   "value": "BANNED_HATE_SPEECH",
 *   "displayName": "Banned - Hate Speech",
 *   "description": "Permanent ban for hate speech or discriminatory content",
 *   "mainCategory": "BANNED",
 *   "requiresSuperAdmin": true,
 *   "allowsReactivation": false
 * }
 * }</pre>
 *
 * @param value              The enum constant name (e.g. {@code "BANNED_HATE_SPEECH"}).
 *                           This is the value sent back to the server when submitting
 *                           a deactivation request.
 *
 * @param displayName        A human-readable label suitable for display in the UI
 *                           (e.g. {@code "Banned - Hate Speech"}).
 *
 * @param description        A short sentence explaining when this category applies.
 *                           Displayed as a tooltip or helper text in the modal.
 *
 * @param mainCategory       The name of the parent {@code DeactivationMainCategory}
 *                           enum constant (e.g. {@code "BANNED"}, {@code "LEGAL"}).
 *                           Used by the front-end to group options under section headers.
 *
 * @param requiresSuperAdmin Whether this category can only be applied by a
 *                           {@code SUPER_ADMIN}. Regular {@code ADMIN} users should
 *                           not see or select categories with this flag set to
 *                           {@code true}.
 *
 * @param allowsReactivation Whether an account deactivated under this category
 *                           can be reactivated later. {@code false} indicates a
 *                           permanent or legally constrained deactivation.
 *
 * @see AdminDeactivationCategory
 * @see be.steby.CoreProject.dl.enums.admin.deactivation.DeactivationMainCategory
 */
public record DeactivationCategoryDTO(
        String  value,
        String  displayName,
        String  description,
        String  mainCategory,
        boolean requiresSuperAdmin,
        boolean allowsReactivation
) {

    /**
     * Creates a {@code DeactivationCategoryDTO} from an {@link AdminDeactivationCategory}
     * enum constant.
     *
     * <p>This is the canonical way to build instances of this DTO — it maps
     * each enum field to its corresponding API field, keeping the mapping
     * logic centralised and easy to maintain.
     *
     * @param cat the source enum constant; must not be {@code null}
     * @return a fully populated DTO ready for serialization
     */
    public static DeactivationCategoryDTO fromEnum(AdminDeactivationCategory cat) {
        return new DeactivationCategoryDTO(
                cat.name(),
                cat.getDisplayName(),
                cat.getDescription(),
                cat.getMainCategory().name(),
                cat.requiresSuperAdminApproval(),
                cat.allowsReactivation()
        );
    }
}