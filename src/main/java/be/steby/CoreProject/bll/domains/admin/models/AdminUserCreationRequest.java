package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.pl.models.admin.UserRegisterForm;

import java.util.Set;

/**
 * Représente une demande de création d'utilisateur par un administrateur.
 * Ce modèle contient toutes les informations nécessaires pour créer un utilisateur
 * dans le contexte d'une opération d'administration.
 */
public record AdminUserCreationRequest(
        /**
         * Nom d'utilisateur unique pour le nouvel utilisateur.
         */
        String username,

        /**
         * Prénom du nouvel utilisateur.
         */
        String firstname,

        /**
         * Nom de famille du nouvel utilisateur.
         */
        String lastname,

        /**
         * Adresse email du nouvel utilisateur.
         */
        String email,

        /**
         * Numéro de téléphone du nouvel utilisateur (optionnel).
         */
        String phoneNumber,

        /**
         * Ensemble des rôles à attribuer au nouvel utilisateur.
         */
        Set<UserRole> userRoles,

        /**
         * Indique si l'utilisateur doit être automatiquement activé.
         */
        boolean autoActivate,

        /**
         * Contexte de la requête (IP, user agent, etc.) pour l'audit.
         */
        RequestContext requestContext
) {

    /**
     * Constructeur avec validation des données.
     */
    public AdminUserCreationRequest {
        // Validation des champs obligatoires
        if (username == null || username.isBlank()) {
            throw new AdminOperationException("Le nom d'utilisateur ne peut pas être vide");
        }
        if (firstname == null || firstname.isBlank()) {
            throw new AdminOperationException("Le prénom ne peut pas être vide");
        }
        if (lastname == null || lastname.isBlank()) {
            throw new AdminOperationException("Le nom de famille ne peut pas être vide");
        }
        if (email == null || email.isBlank()) {
            throw new AdminOperationException("L'adresse email ne peut pas être vide");
        }
        if (userRoles == null || userRoles.isEmpty()) {
            throw new AdminOperationException("Au moins un rôle doit être attribué à l'utilisateur");
        }
        if (requestContext == null) {
            throw new AdminOperationException("Le contexte de requête est requis pour l'audit");
        }

        // Validation des rôles - s'assurer qu'il y a au minimum USER
        if (!userRoles.contains(UserRole.USER)) {
            throw new AdminOperationException("Le rôle USER doit être attribué à tout nouvel utilisateur");
        }
    }

    /**
     * Crée une requête de création d'utilisateur à partir d'un formulaire de la couche présentation.
     *
     * @param form Formulaire de création d'utilisateur
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance d'AdminUserCreationRequest
     */
    public static AdminUserCreationRequest fromForm(UserRegisterForm form, RequestContext requestContext) {
        return new AdminUserCreationRequest(
                form.username(),
                form.firstname(),
                form.lastname(),
                form.email(),
                form.phoneNumber(),
                form.userRoles(),
                form.autoActivate(),
                requestContext
        );
    }

    /**
     * Crée une requête de création d'utilisateur simple avec activation automatique.
     *
     * @param username Nom d'utilisateur
     * @param firstname Prénom
     * @param lastname Nom de famille
     * @param email Email
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance avec les rôles par défaut (USER)
     */
    public static AdminUserCreationRequest forBasicUser(
            String username, String firstname, String lastname, String email, RequestContext requestContext) {
        return new AdminUserCreationRequest(
                username,
                firstname,
                lastname,
                email,
                null, // Pas de téléphone
                Set.of(UserRole.USER), // Rôle par défaut
                true, // Activation automatique
                requestContext
        );
    }

    /**
     * Vérifie si la création demande des privilèges SUPER_ADMIN.
     *
     * @return true si l'utilisateur à créer aura le rôle SUPER_ADMIN
     */
    public boolean requiresSuperAdminPrivileges() {
        return userRoles.contains(UserRole.SUPER_ADMIN);
    }

    /**
     * Vérifie si la création demande des privilèges d'administration.
     *
     * @return true si l'utilisateur à créer aura des rôles d'administration
     */
    public boolean requiresAdminPrivileges() {
        return userRoles.stream().anyMatch(role ->
                role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR
        );
    }
}