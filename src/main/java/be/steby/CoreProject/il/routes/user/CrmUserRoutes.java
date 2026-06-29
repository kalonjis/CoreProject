package be.steby.CoreProject.il.routes.user;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for CRM user lookup endpoints.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/users</pre>
 *
 * @see be.steby.CoreProject.pl.domains.user.controllers.CrmUserController
 */
public final class CrmUserRoutes {

    private CrmUserRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static final String BASE = "/api/crm/users";

    private static final String[] LOOKUP_ROUTES = {
            BASE + "/commercials"   // GET — list of active commercials for assignment dropdowns
    };

    /** All CRM user routes — require {@code COMMERCIAL} or {@code ADMIN} authority. */
    public static final String[] COMMERCIAL = concatenate(LOOKUP_ROUTES);

    public static final String[] CSRF_IGNORE = {};
}
