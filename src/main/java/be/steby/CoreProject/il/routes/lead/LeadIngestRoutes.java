package be.steby.CoreProject.il.routes.lead;

/**
 * Route definitions for the public webhook lead ingest endpoint.
 *
 * <p>These routes are publicly accessible (no JWT) and secured by
 * per-platform signature verification instead.</p>
 */
public final class LeadIngestRoutes {

    private LeadIngestRoutes() {}

    public static final String BASE = "/api/public/leads/ingest";

    public static final String[] PUBLIC = {
            BASE + "/**"
    };

    public static final String[] CSRF_IGNORE = {
            BASE + "/**"
    };
}
