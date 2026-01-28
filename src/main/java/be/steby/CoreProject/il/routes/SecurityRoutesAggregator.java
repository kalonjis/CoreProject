package be.steby.CoreProject.il.routes;

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
import be.steby.CoreProject.il.routes.storage.StorageRoutes;
import be.steby.CoreProject.il.routes.swagger.SwaggerRoutes;
import be.steby.CoreProject.il.routes.debug.DebugRoutes;

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
            ProfileRoutes.PUBLIC
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
            ProfileRoutes.AUTHENTICATED
    );

    // ========== ADMIN ROUTES ==========

    public static final String[] ADMIN_ROUTES = concatenate(
            AdminRoutes.ADMIN,
            ActuatorRoutes.ADMIN
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
            ProfileRoutes.CSRF_IGNORE
    );

    private SecurityRoutesAggregator() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}