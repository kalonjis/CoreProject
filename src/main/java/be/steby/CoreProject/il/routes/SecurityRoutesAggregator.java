package be.steby.CoreProject.il.routes;

import be.steby.CoreProject.il.routes.activitylog.ActivityLogRoutes;
import be.steby.CoreProject.il.routes.calendar.CalendarRoutes;
import be.steby.CoreProject.il.routes.gdpr.GdprRoutes;
import be.steby.CoreProject.il.routes.contact.CrmContactRoutes;
import be.steby.CoreProject.il.routes.deal.CrmDealRoutes;
import be.steby.CoreProject.il.routes.lead.CrmLeadRoutes;
import be.steby.CoreProject.il.routes.organisation.CrmOrganisationRoutes;
import be.steby.CoreProject.il.routes.commercialaction.CrmCommercialActionRoutes;
import be.steby.CoreProject.il.routes.dashboard.CrmDashboardRoutes;
import be.steby.CoreProject.il.routes.interaction.CrmInteractionRoutes;
import be.steby.CoreProject.il.routes.timeline.CrmTimelineRoutes;
import be.steby.CoreProject.il.routes.pipeline.CrmPipelineRoutes;
import be.steby.CoreProject.il.routes.lead.LeadIngestRoutes;
import be.steby.CoreProject.il.routes.lead.LeadRoutes;
import be.steby.CoreProject.il.routes.notification.NotificationRoutes;
import be.steby.CoreProject.il.routes.password.PasswordRoutes;
import be.steby.CoreProject.il.routes.auth.AuthRoutes;
import be.steby.CoreProject.il.routes.auth.TwoFactorRoutes;
import be.steby.CoreProject.il.routes.account.AccountRoutes;
import be.steby.CoreProject.il.routes.device.DeviceRoutes;
import be.steby.CoreProject.il.routes.address.AddressRoutes;
import be.steby.CoreProject.il.routes.emailchange.EmailChangeRoutes;
import be.steby.CoreProject.il.routes.admin.AdminRoutes;
import be.steby.CoreProject.il.routes.actuator.ActuatorRoutes;
import be.steby.CoreProject.il.routes.monitoring.MonitoringRoutes;
import be.steby.CoreProject.il.routes.profile.ProfileRoutes;
import be.steby.CoreProject.il.routes.sport.SportRoutes;
import be.steby.CoreProject.il.routes.storage.StorageRoutes;
import be.steby.CoreProject.il.routes.swagger.SwaggerRoutes;
import be.steby.CoreProject.il.routes.debug.DebugRoutes;
import be.steby.CoreProject.il.routes.changelog.CrmChangeLogRoutes;
import be.steby.CoreProject.il.routes.tag.CrmTagRoutes;
import be.steby.CoreProject.il.routes.user.CrmUserRoutes;
import be.steby.CoreProject.il.routes.supportticket.CrmSupportTicketRoutes;
import be.steby.CoreProject.il.routes.call.CrmCallRoutes;
import be.steby.CoreProject.il.routes.supportticket.PublicSupportTicketRoutes;
import be.steby.CoreProject.il.routes.telephony.CrmTwilioRoutes;
import be.steby.CoreProject.il.routes.telephony.CrmSipConfigRoutes;
import be.steby.CoreProject.il.routes.telephony.CrmTelephonyRoutes;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Central aggregator for all security routes configuration.
 *
 * This class ONLY aggregates routes from domain-specific configurations.
 * No hardcoded URLs here - all routes are defined in their respective domain files.
 *
 * @see AuthRoutes
 * @see TwoFactorRoutes
 * @see AccountRoutes
 * @see PasswordRoutes
 * @see DeviceRoutes
 * @see AddressRoutes
 * @see EmailChangeRoutes
 * @see AdminRoutes
 * @see ActuatorRoutes
 * @see MonitoringRoutes
 * @see SwaggerRoutes
 * @see DebugRoutes
 * @see CrmLeadRoutes
 * @see CrmDealRoutes
 */
public final class SecurityRoutesAggregator {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC_ROUTES = concatenate(
            AuthRoutes.PUBLIC,
            TwoFactorRoutes.PUBLIC,
            AccountRoutes.PUBLIC,
            PasswordRoutes.PUBLIC,
            DeviceRoutes.PUBLIC,
            AddressRoutes.PUBLIC,
            EmailChangeRoutes.PUBLIC,
            ActuatorRoutes.PUBLIC,
            MonitoringRoutes.PUBLIC,
            SwaggerRoutes.PUBLIC,
            DebugRoutes.PUBLIC,
            ProfileRoutes.PUBLIC,
            LeadRoutes.PUBLIC,
            LeadIngestRoutes.PUBLIC,
            CalendarRoutes.PUBLIC,
            GdprRoutes.PUBLIC,
            NotificationRoutes.PUBLIC,
            PublicSupportTicketRoutes.PUBLIC,
            CrmTwilioRoutes.PUBLIC
    );

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED_ROUTES = concatenate(
            AuthRoutes.AUTHENTICATED,
            TwoFactorRoutes.AUTHENTICATED,
            AccountRoutes.AUTHENTICATED,
            PasswordRoutes.AUTHENTICATED,
            DeviceRoutes.AUTHENTICATED,
            AddressRoutes.AUTHENTICATED,
            EmailChangeRoutes.AUTHENTICATED,
            ActuatorRoutes.AUTHENTICATED,
            MonitoringRoutes.AUTHENTICATED,
            SwaggerRoutes.AUTHENTICATED,
            DebugRoutes.AUTHENTICATED,
            StorageRoutes.AUTHENTICATED,
            ProfileRoutes.AUTHENTICATED,
            SportRoutes.AUTHENTICATED,
            CalendarRoutes.AUTHENTICATED,
            NotificationRoutes.AUTHENTICATED,
            GdprRoutes.AUTHENTICATED,
            ActivityLogRoutes.AUTHENTICATED

    );

    // ========== COMMERCIAL ROUTES ==========

    /**
     * Routes requiring COMMERCIAL or ADMIN authority.
     *
     * Used in SecurityConfig with:
     * .requestMatchers(COMMERCIAL_ROUTES).hasAnyAuthority("COMMERCIAL", "ADMIN")
     */
    public static final String[] COMMERCIAL_ROUTES = concatenate(
            CrmLeadRoutes.COMMERCIAL,
            CrmContactRoutes.COMMERCIAL,
            CrmOrganisationRoutes.COMMERCIAL,
            CrmDealRoutes.COMMERCIAL,
            CrmPipelineRoutes.COMMERCIAL,
            CrmInteractionRoutes.COMMERCIAL,
            CrmTimelineRoutes.COMMERCIAL,
            CrmCommercialActionRoutes.COMMERCIAL,
            CrmDashboardRoutes.COMMERCIAL,
            CrmUserRoutes.COMMERCIAL,
            CrmTagRoutes.COMMERCIAL,
            CrmChangeLogRoutes.COMMERCIAL,
            CrmSupportTicketRoutes.COMMERCIAL,
            CrmCallRoutes.COMMERCIAL,
            CrmTwilioRoutes.COMMERCIAL,
            CrmSipConfigRoutes.COMMERCIAL,
            CrmTelephonyRoutes.COMMERCIAL
    );

    // ========== ADMIN ROUTES ==========

    public static final String[] ADMIN_ROUTES = concatenate(
            AdminRoutes.ADMIN,
            ActuatorRoutes.ADMIN,
            CalendarRoutes.ADMIN,
            ActivityLogRoutes.ADMIN,
            CrmSipConfigRoutes.ADMIN
    );

    // ========== MONITORING ROUTES ==========

    /**
     * Routes requiring MONITORING or SUPER_ADMIN authority.
     *
     * Used in SecurityConfig with:
     * .requestMatchers(MONITORING_AUTHORIZED_ROUTES).hasAnyAuthority("MONITORING", "SUPER_ADMIN")
     */
    public static final String[] MONITORING_AUTHORIZED_ROUTES = MonitoringRoutes.MONITORING_AUTHORIZED;

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * Each domain is responsible for defining its own CSRF_IGNORE routes.
     * This aggregator only combines them.
     *
     * ⚠️ TODO PRODUCTION: Each domain's CSRF_IGNORE should be reviewed
     * and dev-only routes should be removed before deployment.
     */
    public static final String[] CSRF_IGNORE = concatenate(
            AuthRoutes.CSRF_IGNORE,
            TwoFactorRoutes.CSRF_IGNORE,
            AccountRoutes.CSRF_IGNORE,
            PasswordRoutes.CSRF_IGNORE,
            DeviceRoutes.CSRF_IGNORE,
            AddressRoutes.CSRF_IGNORE,
            EmailChangeRoutes.CSRF_IGNORE,
            AdminRoutes.CSRF_IGNORE,
            ActuatorRoutes.CSRF_IGNORE,
            MonitoringRoutes.CSRF_IGNORE,
            SwaggerRoutes.CSRF_IGNORE,
            DebugRoutes.CSRF_IGNORE,
            StorageRoutes.CSRF_IGNORE,
            ProfileRoutes.CSRF_IGNORE,
            SportRoutes.CSRF_IGNORE,
            LeadRoutes.CSRF_IGNORE,
            LeadIngestRoutes.CSRF_IGNORE,
            CrmLeadRoutes.CSRF_IGNORE,
            CrmContactRoutes.CSRF_IGNORE,
            CrmOrganisationRoutes.CSRF_IGNORE,
            CrmDealRoutes.CSRF_IGNORE,
            CrmPipelineRoutes.CSRF_IGNORE,
            CrmInteractionRoutes.CSRF_IGNORE,
            CrmTimelineRoutes.CSRF_IGNORE,
            CrmCommercialActionRoutes.CSRF_IGNORE,
            CrmDashboardRoutes.CSRF_IGNORE,
            CrmUserRoutes.CSRF_IGNORE,
            CrmTagRoutes.CSRF_IGNORE,
            CrmChangeLogRoutes.CSRF_IGNORE,
            CrmSupportTicketRoutes.CSRF_IGNORE,
            CrmCallRoutes.CSRF_IGNORE,
            PublicSupportTicketRoutes.CSRF_IGNORE,
            CrmTwilioRoutes.CSRF_IGNORE,
            CrmSipConfigRoutes.CSRF_IGNORE,
            CrmTelephonyRoutes.CSRF_IGNORE,
            CalendarRoutes.CSRF_IGNORE,
            GdprRoutes.CSRF_IGNORE,
            NotificationRoutes.CSRF_IGNORE
    );

    private SecurityRoutesAggregator() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}