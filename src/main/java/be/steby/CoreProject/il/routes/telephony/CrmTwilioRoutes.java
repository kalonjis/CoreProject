package be.steby.CoreProject.il.routes.telephony;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the Twilio Voice integration.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/telephony/twilio</pre>
 *
 * <h3>Access model</h3>
 * <ul>
 *   <li>{@code /token} — requires {@code COMMERCIAL} or {@code ADMIN} authority</li>
 *   <li>{@code /twiml} and {@code /webhook} — public (no JWT), secured by Twilio signature</li>
 * </ul>
 *
 * @see be.steby.CoreProject.pl.domains.telephony.twilio.controllers.TwilioController
 */
public final class CrmTwilioRoutes {

    private CrmTwilioRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static final String BASE = "/api/crm/telephony/twilio";

    // ── Public routes (Twilio servers, no JWT — protected by X-Twilio-Signature) ──

    private static final String[] TWILIO_PUBLIC_ROUTES = {
            BASE + "/twiml",
            BASE + "/webhook",
            BASE + "/token/test"   // TODO dev-only — remove before prod
    };

    // ── Commercial routes (frontend, JWT required) ──

    private static final String[] TWILIO_COMMERCIAL_ROUTES = {
            BASE + "/token"
    };

    /** Routes accessible without JWT — Twilio servers call these. */
    public static final String[] PUBLIC = TWILIO_PUBLIC_ROUTES;

    /** Routes requiring {@code COMMERCIAL} or {@code ADMIN} authority. */
    public static final String[] COMMERCIAL = TWILIO_COMMERCIAL_ROUTES;

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>Twilio webhook and TwiML endpoints are server-to-server calls
     * (no browser session) and must bypass CSRF.</p>
     *
     * <p>⚠️ TODO PRODUCTION: Set CSRF_IGNORE to {@code TWILIO_PUBLIC_ROUTES} only.</p>
     */
    public static final String[] CSRF_IGNORE = concatenate(TWILIO_PUBLIC_ROUTES, TWILIO_COMMERCIAL_ROUTES);
}
