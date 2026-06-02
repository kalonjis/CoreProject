package be.steby.CoreProject.il.routes.telephony;

/**
 * Route definitions for the general CRM telephony domain.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/telephony</pre>
 */
public final class CrmTelephonyRoutes {

    private CrmTelephonyRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    private static final String[] COMMERCIAL_ROUTES = {
            "/api/crm/telephony/my-provider"
    };

    public static final String[] COMMERCIAL  = COMMERCIAL_ROUTES;
    public static final String[] CSRF_IGNORE = COMMERCIAL_ROUTES;
}
