package be.steby.CoreProject.dl.enums;

public enum ReactivationPolicy {
    NEVER,              // Personne ne peut réactiver
    SELF_SERVICE,       // L'utilisateur peut se réactiver
    ADMIN_ONLY,         // Seulement les admins
    SUPER_ADMIN_ONLY    // Seulement les super admins
}