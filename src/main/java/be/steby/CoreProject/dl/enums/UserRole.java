package be.steby.CoreProject.dl.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Énumération des rôles utilisateur avec logique de hiérarchie intégrée.
 * Hiérarchie (du plus élevé au plus bas) : SUPER_ADMIN > ADMIN > MODERATOR > USER > GUEST
 */
public enum UserRole {
    SUPER_ADMIN(0),
    ADMIN(1),
    MODERATOR(2),
    USER(3),
    GUEST(4);

    private final int hierarchyLevel;

    UserRole(int hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * Obtient le niveau hiérarchique du rôle (plus petit = plus élevé).
     *
     * @return Le niveau hiérarchique
     */
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * Vérifie si ce rôle est plus élevé que l'autre dans la hiérarchie.
     *
     * @param other L'autre rôle à comparer
     * @return true si ce rôle est plus élevé
     */
    public boolean isHigherThan(UserRole other) {
        if (other == null) return true;
        return this.hierarchyLevel < other.hierarchyLevel;
    }

    /**
     * Vérifie si ce rôle est au moins au niveau du rôle minimum requis.
     *
     * @param minimumRole Le rôle minimum requis
     * @return true si ce rôle satisfait le minimum requis
     */
    public boolean isAtLeast(UserRole minimumRole) {
        if (minimumRole == null) return true;
        return this.hierarchyLevel <= minimumRole.hierarchyLevel;
    }

    /**
     * Vérifie si ce rôle est égal ou inférieur à l'autre dans la hiérarchie.
     *
     * @param other L'autre rôle à comparer
     * @return true si ce rôle est égal ou inférieur
     */
    public boolean isAtMost(UserRole other) {
        if (other == null) return false;
        return this.hierarchyLevel >= other.hierarchyLevel;
    }

    /**
     * Trouve le rôle le plus élevé dans un ensemble de rôles.
     *
     * @param roles L'ensemble des rôles
     * @return Le rôle le plus élevé, ou GUEST si l'ensemble est vide/null
     */
    public static UserRole getHighestRole(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return GUEST;
        }

        return roles.stream()
                .min((r1, r2) -> Integer.compare(r1.hierarchyLevel, r2.hierarchyLevel))
                .orElse(GUEST);
    }

    /**
     * Trouve le rôle le moins élevé dans un ensemble de rôles.
     *
     * @param roles L'ensemble des rôles
     * @return Le rôle le moins élevé, ou GUEST si l'ensemble est vide/null
     */
    public static UserRole getLowestRole(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return GUEST;
        }

        return roles.stream()
                .max((r1, r2) -> Integer.compare(r1.hierarchyLevel, r2.hierarchyLevel))
                .orElse(GUEST);
    }

    /**
     * Vérifie si un ensemble de rôles contient au moins le rôle minimum requis.
     *
     * @param roles L'ensemble des rôles à vérifier
     * @param minimumRole Le rôle minimum requis
     * @return true si l'ensemble contient un rôle suffisant
     */
    public static boolean hasMinimumRole(Set<UserRole> roles, UserRole minimumRole) {
        if (roles == null || roles.isEmpty()) {
            return minimumRole == GUEST;
        }

        UserRole highestRole = getHighestRole(roles);
        return highestRole.isAtLeast(minimumRole);
    }

    /**
     * Obtient tous les rôles inférieurs ou égaux à ce rôle.
     *
     * @return L'ensemble des rôles inférieurs ou égaux
     */
    public Set<UserRole> getRolesAtOrBelow() {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.hierarchyLevel >= this.hierarchyLevel)
                .collect(Collectors.toSet());
    }

    /**
     * Obtient tous les rôles supérieurs ou égaux à ce rôle.
     *
     * @return L'ensemble des rôles supérieurs ou égaux
     */
    public Set<UserRole> getRolesAtOrAbove() {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.hierarchyLevel <= this.hierarchyLevel)
                .collect(Collectors.toSet());
    }

    /**
     * Vérifie si ce rôle peut agir sur un autre rôle selon la hiérarchie.
     * Règle générale : un rôle peut agir sur les rôles inférieurs.
     *
     * @param targetRole Le rôle cible
     * @return true si ce rôle peut agir sur le rôle cible
     */
    public boolean canActOn(UserRole targetRole) {
        if (targetRole == null) return false;
        return this.isHigherThan(targetRole) || this.equals(targetRole);
    }

    /**
     * Vérifie si ce rôle est un rôle administratif.
     *
     * @return true si le rôle a des privilèges administratifs
     */
    public boolean isAdministrative() {
        return this == SUPER_ADMIN || this == ADMIN;  // || this == MODERATOR;
    }

    /**
     * Vérifie si ce rôle est un rôle de super-administration.
     *
     * @return true si le rôle est SUPER_ADMIN
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * Obtient une description du rôle.
     *
     * @return Description lisible du rôle
     */
    public String getDescription() {
        return switch (this) {
            case SUPER_ADMIN -> "Super Administrateur - Accès complet au système";
            case ADMIN -> "Administrateur - Gestion des utilisateurs et du contenu";
            case MODERATOR -> "Modérateur - Modération du contenu et des interactions";
            case USER -> "Utilisateur - Accès standard aux fonctionnalités";
            case GUEST -> "Invité - Accès limité en lecture seule";
        };
    }

    // Méthode existante conservée pour compatibilité
    public static Set<UserRole> setRoles(UserRole topRole) {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.ordinal() >= topRole.ordinal())
                .collect(Collectors.toSet());
    }
}