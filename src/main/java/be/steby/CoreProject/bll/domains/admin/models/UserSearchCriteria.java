package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Set;

/**
 * Représente les critères de recherche pour les utilisateurs dans le contexte d'administration.
 * Ce modèle permet de construire des requêtes de recherche complexes avec différents filtres.
 */
public record UserSearchCriteria(
        /**
         * Recherche textuelle générale (nom d'utilisateur, nom, prénom, email).
         */
        String query,

        /**
         * Filtre par nom d'utilisateur spécifique.
         */
        String username,

        /**
         * Filtre par prénom.
         */
        String firstname,

        /**
         * Filtre par nom de famille.
         */
        String lastname,

        /**
         * Filtre par adresse email.
         */
        String email,

        /**
         * Filtre par numéro de téléphone.
         */
        String phoneNumber,

        /**
         * Filtre par rôles utilisateur.
         */
        Set<UserRole> roles,

        /**
         * Filtre par statut d'activation.
         */
        Boolean enabled,

        /**
         * Filtre par date de création (à partir de).
         */
        LocalDate createdAfter,

        /**
         * Filtre par date de création (jusqu'à).
         */
        LocalDate createdBefore,

        /**
         * Filtre par date de dernière connexion (à partir de).
         */
        LocalDate lastLoginAfter,

        /**
         * Filtre par date de dernière connexion (jusqu'à).
         */
        LocalDate lastLoginBefore,

        /**
         * Informations de pagination.
         */
        Pageable pageable
) {

    /**
     * Crée des critères de recherche simple par requête textuelle.
     *
     * @param query Texte de recherche
     * @param pageable Informations de pagination
     * @return Nouveaux critères de recherche
     */
    public static UserSearchCriteria simpleSearch(String query, Pageable pageable) {
        return new UserSearchCriteria(
                query, null, null, null, null, null, null, null,
                null, null, null, null, pageable
        );
    }

    /**
     * Crée des critères de recherche par critères spécifiques.
     *
     * @param username Nom d'utilisateur
     * @param firstname Prénom
     * @param lastname Nom de famille
     * @param email Email
     * @param phoneNumber Téléphone
     * @param pageable Informations de pagination
     * @return Nouveaux critères de recherche
     */
    public static UserSearchCriteria detailedSearch(
            String username, String firstname, String lastname,
            String email, String phoneNumber, Pageable pageable) {
        return new UserSearchCriteria(
                null, username, firstname, lastname, email, phoneNumber,
                null, null, null, null, null, null, pageable
        );
    }

    /**
     * Crée des critères de recherche par rôles.
     *
     * @param roles Ensemble des rôles à rechercher
     * @param pageable Informations de pagination
     * @return Nouveaux critères de recherche
     */
    public static UserSearchCriteria byRoles(Set<UserRole> roles, Pageable pageable) {
        return new UserSearchCriteria(
                null, null, null, null, null, null, roles, null,
                null, null, null, null, pageable
        );
    }

    /**
     * Crée des critères de recherche par statut d'activation.
     *
     * @param activated Statut d'activation recherché
     * @param pageable Informations de pagination
     * @return Nouveaux critères de recherche
     */
    public static UserSearchCriteria byActivationStatus(Boolean activated, Pageable pageable) {
        return new UserSearchCriteria(
                null, null, null, null, null, null, null, activated,
                null, null, null, null, pageable
        );
    }

    /**
     * Vérifie si une recherche textuelle simple est demandée.
     *
     * @return true si seule la query est définie
     */
    public boolean isSimpleSearch() {
        return query != null && !query.isBlank() &&
                username == null && firstname == null && lastname == null &&
                email == null && phoneNumber == null && roles == null;
    }

    /**
     * Vérifie si des critères détaillés sont utilisés.
     *
     * @return true si des critères spécifiques sont définis
     */
    public boolean hasDetailedCriteria() {
        return username != null || firstname != null || lastname != null ||
                email != null || phoneNumber != null;
    }

    /**
     * Vérifie si des filtres par rôles sont appliqués.
     *
     * @return true si des rôles sont spécifiés
     */
    public boolean hasRoleFilters() {
        return roles != null && !roles.isEmpty();
    }

    /**
     * Vérifie si des filtres temporels sont appliqués.
     *
     * @return true si des dates sont spécifiées
     */
    public boolean hasDateFilters() {
        return createdAfter != null || createdBefore != null ||
                lastLoginAfter != null || lastLoginBefore != null;
    }

    /**
     * Vérifie si aucun critère de recherche n'est défini.
     *
     * @return true si tous les critères sont vides
     */
    public boolean isEmpty() {
        return (query == null || query.isBlank()) &&
                (username == null || username.isBlank()) &&
                (firstname == null || firstname.isBlank()) &&
                (lastname == null || lastname.isBlank()) &&
                (email == null || email.isBlank()) &&
                (phoneNumber == null || phoneNumber.isBlank()) &&
                (roles == null || roles.isEmpty()) &&
                enabled == null &&
                !hasDateFilters();
    }

    /**
     * Crée une nouvelle instance avec une requête textuelle ajoutée.
     *
     * @param newQuery Nouvelle requête textuelle
     * @return Nouveaux critères avec la requête mise à jour
     */
    public UserSearchCriteria withQuery(String newQuery) {
        return new UserSearchCriteria(
                newQuery, username, firstname, lastname, email, phoneNumber,
                roles, enabled, createdAfter, createdBefore,
                lastLoginAfter, lastLoginBefore, pageable
        );
    }

    /**
     * Crée une nouvelle instance avec des rôles ajoutés.
     *
     * @param newRoles Nouveaux rôles à filtrer
     * @return Nouveaux critères avec les rôles mis à jour
     */
    public UserSearchCriteria withRoles(Set<UserRole> newRoles) {
        return new UserSearchCriteria(
                query, username, firstname, lastname, email, phoneNumber,
                newRoles, enabled, createdAfter, createdBefore,
                lastLoginAfter, lastLoginBefore, pageable
        );
    }
}