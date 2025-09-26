package be.steby.CoreProject.il.utils;

public class SecurityConstants {
    // Liste statique des chemins à ignorer pour la protection CSRF
    public static final String[] CSRF_IGNORE_PATHS = {
            "/api/debug/**", // for test only - not for production!!!!
            "/api/test/device-security/**",
            "/api/auth/**",
            "/api/user/device/current",
            "/api/user/device/my-device/**",
            "/api/user/device/my-devices-list",
            "/api/user/device/confirm/**",
            "/api/user/device/reject/**",
            "/api/user/device/update-trust-level/**",
            "api/user/device/request-confirmation",
            "api/user/device/disconnect/**",
            "api/user/device/disconnect-all-others",
            "/api/account/**",
            "/api/password/request-password-reset",
            "/api/password/reset-password",
            "/api/password/request-password-token",
            "/api/password/change-password",
            "/api/email-address-change/request",
            "/api/email-address-change/cancel",
            "/api/email-address-change/verification",
            "/api/email-address-change/confirmation",
            "/api/admin/users",
            "/api/admin/users/activate/**",
            "/api/admin/users/deactivate/**",
            "/api/admin/users/force-reset-password/**",
            "/api/admin/users/grant-role/**",
            "/api/admin/users/revoke-role/**",
            "/api/admin/users/gdpr-deletion/**",
            "/api/admin/cache/device/**",
            "/api/admin/cache/use/**"
    };

    // Vous pouvez aussi définir d'autres constantes liées à la sécurité
    public static final String[] PUBLIC_ROUTES = {
            "/api/debug/**",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh-token",
            "/api/auth/logout",
            "/api/account/signup",
            "/api/account/activate/**",
            "api/account/activation-request/**",
            "/api/account/reactivation-request",
            "/api/account/reactivation/**",
            "/api/password/**",
            "/api/user/device/confirm/**",
            "/api/user/device/reject/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    public static final String[] ADMIN_ROUTES = {
            "/api/admin/**",
            "/api/security/logs/user/**",
            "/api/admin/device/list/user/**",
            "/api/device/update-trust-level/**",
            "/api/admin/users/grant-role/**",
            "/api/admin/users/revoke-role/**",
            "/api/admin/cache/device/**",
            "/api/admin/cache/use/**"

    };

}
