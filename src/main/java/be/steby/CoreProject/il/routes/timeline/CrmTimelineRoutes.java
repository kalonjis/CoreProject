package be.steby.CoreProject.il.routes.timeline;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the unified CRM activity timeline domain.
 *
 * <p>The timeline merges {@link be.steby.CoreProject.dl.entities.crm.Interaction}
 * entries and completed {@link be.steby.CoreProject.dl.entities.crm.CommercialAction}
 * entries into a single chronological feed.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/timeline</pre>
 *
 * @see be.steby.CoreProject.pl.domains.timeline.controllers.CrmTimelineController
 */
public final class CrmTimelineRoutes {

    private CrmTimelineRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static final String BASE = "/api/crm/timeline";

    private static final String[] LIST_ROUTES = {
            BASE + "/**"  // GET  — deal, contact, lead timeline
    };

    /**
     * All CRM timeline routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = concatenate(LIST_ROUTES);

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>⚠️ DEV ONLY — remove all in production.</p>
     *
     * <p>⚠️ TODO PRODUCTION: Set this to empty array: {@code {}}</p>
     */
    public static final String[] CSRF_IGNORE = {};
}
