package be.steby.CoreProject.dl.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User role enumeration with hierarchy levels.
 *
 * Hierarchy (from highest to lowest): SUPER_ADMIN > ADMIN > MODERATOR > USER > GUEST
 *
 * This enum defines WHAT roles exist and their hierarchy.
 * All role checking logic belongs in the User entity or service layer.
 *
 * @author Steby Core Team
 * @version 4.0 - Ultra Minimal (removed all redundant methods)
 * @since 2025-01
 */
public enum UserRole {
    MONITORING(-1),
    SUPER_ADMIN(0),
    ADMIN(1),
    MODERATOR(2),
    USER(3),
    GUEST(4);

    private final int hierarchyLevel;

    UserRole(int hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    // ===============================
    // ✅ METHOD 1: Hierarchy Level (Core Metadata)
    // ===============================

    /**
     * Gets the hierarchy level of this role (lower value = higher privilege).
     *
     * Used for: Direct role comparisons in AdminPermissionValidator
     *
     * @return The hierarchy level (0 = SUPER_ADMIN, 4 = GUEST)
     */
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    // ===============================
    // ✅ METHOD 2: Role Set Generation (Initialization)
    // ===============================

    /**
     * Creates a set of all roles from this role downward in the hierarchy.
     *
     * Used for:
     * - DataInitializer (test data creation)
     * - User constructors (when creating users with a top role)
     * - AdminUserCreationService (applying role hierarchy)
     *
     * Example: setRoles(ADMIN) returns [ADMIN, MODERATOR, USER, GUEST]
     *
     * @param topRole The highest role to include
     * @return Set of roles from topRole downward
     */
    public static Set<UserRole> setRoles(UserRole topRole) {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.ordinal() >= topRole.ordinal())
                .collect(Collectors.toSet());
    }

    // ===============================
    // ✅ METHOD 3: Description (Display Only)
    // ===============================

    /**
     * Gets a human-readable description of the role.
     *
     * Used for: UI/API responses for displaying role information to users
     *
     * @return Localized description of the role
     */
    public String getDescription() {
        return switch (this) {
            case MONITORING -> "Technical rôle only";
            case SUPER_ADMIN -> "Super Administrator - Full system access";
            case ADMIN -> "Administrator - User and content management";
            case MODERATOR -> "Moderator - Content and interaction moderation";
            case USER -> "User - Standard feature access";
            case GUEST -> "Guest - Limited read-only access";
        };
    }

    public static UserRole getHighestRole(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return GUEST;
        }

        return roles.stream()
                .min((r1, r2) -> Integer.compare(r1.hierarchyLevel, r2.hierarchyLevel))
                .orElse(GUEST);
    }
}
