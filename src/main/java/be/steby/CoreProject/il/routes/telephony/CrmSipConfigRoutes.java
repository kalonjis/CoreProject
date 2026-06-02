package be.steby.CoreProject.il.routes.telephony;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the per-commercial SIP configuration domain.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/telephony/sip</pre>
 *
 * <h3>Access model</h3>
 * <ul>
 *   <li>{@code /connection} — requires {@code COMMERCIAL} or {@code ADMIN} authority</li>
 *   <li>All other routes — require {@code ADMIN} authority (enforced via {@code @PreAuthorize})</li>
 * </ul>
 *
 * @see be.steby.CoreProject.pl.domains.telephony.sip.controllers.CommercialSipConfigController
 */
public final class CrmSipConfigRoutes {

    private CrmSipConfigRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static final String BASE = "/api/crm/telephony/sip";

    private static final String[] COMMERCIAL_ROUTES = {
            BASE + "/connection"
    };

    private static final String[] ADMIN_ROUTES = {
            BASE,
            BASE + "/**"
    };

    /** Routes requiring {@code COMMERCIAL} or {@code ADMIN} authority. */
    public static final String[] COMMERCIAL = COMMERCIAL_ROUTES;

    /** Routes requiring {@code ADMIN} authority. */
    public static final String[] ADMIN = ADMIN_ROUTES;

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>⚠️ DEV ONLY — remove all in production.</p>
     *
     * <p>⚠️ TODO PRODUCTION: Set this to empty array: {@code {}}</p>
     */
    public static final String[] CSRF_IGNORE = concatenate(COMMERCIAL_ROUTES, ADMIN_ROUTES);
}
