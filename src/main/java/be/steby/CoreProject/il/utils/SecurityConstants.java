package be.steby.CoreProject.il.utils;

public class SecurityConstants {
    // Liste statique des chemins à ignorer pour la protection CSRF
    public static final String[] CSRF_IGNORE_PATHS = {
            "/api/auth/**",
            "/api/device/confirm/**",
            "/api/device/reject/**",
            "/api/account-confirmation/**",
            "/api/password/request-password-reset",
            "/api/password/reset-password",
            "/api/password/request-password-token",
            "/api/password/change-password",
            "/api/admin/users",
            "/api/admin/users/activate/**",
            "/api/admin/users/deactivate/**",
            "/api/admin/users/force-reset-password/**",
            "/api/device/update-trust-level/**",
            "/api/admin/users/grant-role/**",
            "/api/admin/users/revoke-role/**"
    };

    // Vous pouvez aussi définir d'autres constantes liées à la sécurité
    public static final String[] PUBLIC_ROUTES = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/refresh-token",
            "/api/auth/logout",
            "/api/account-confirmation/**",
            "/api/password/**",
            "/api/device/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    public static final String[] ADMIN_ROUTES = {
            "/api/admin/**",
            "/api/security/logs/user/**",
            "/api/admin/device/list/user/**",
            "/api/device/update-trust-level/**",
            "/api/admin/users/grant-role/**",
            "/api/admin/users/revoke-role/**"
    };

}
