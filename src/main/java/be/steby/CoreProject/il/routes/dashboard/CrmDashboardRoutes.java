package be.steby.CoreProject.il.routes.dashboard;

/**
 * Route definitions for the CRM dashboard statistics endpoint.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/stats</pre>
 *
 * <h3>Access control</h3>
 * <p>Read-only endpoint accessible to {@code COMMERCIAL} and {@code ADMIN} users.</p>
 *
 * @see be.steby.CoreProject.pl.domains.dashboard.controllers.CrmDashboardController
 */
public final class CrmDashboardRoutes {

    private CrmDashboardRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static final String BASE = "/api/crm/stats";

    public static final String[] COMMERCIAL = { BASE };

    /** ⚠️ DEV ONLY — set to {@code {}} in production. */
    public static final String[] CSRF_IGNORE = { BASE };
}
