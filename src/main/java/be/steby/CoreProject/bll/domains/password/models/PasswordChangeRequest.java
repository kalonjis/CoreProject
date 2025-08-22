package be.steby.CoreProject.bll.domains.password.models;

import be.steby.CoreProject.bll.domains.password.exceptions.PasswordValidationException;
import be.steby.CoreProject.pl.models.password.ChangePasswordForm;

/**
 * Représente une demande de changement de mot de passe dans le domaine métier.
 * Ce DTO est utilisé par la couche service et ne contient pas d'annotations
 * de validation spécifiques à la couche présentation.
 * <p>
 * Il contient uniquement les informations nécessaires pour le traitement
 * métier du changement de mot de passe.
 */
public record PasswordChangeRequest(
        /**
         * Mot de passe actuel de l'utilisateur.
         * Utilisé pour vérifier l'identité de l'utilisateur avant de procéder au changement.
         */
        String currentPassword,

        /**
         * Nouveau mot de passe que l'utilisateur souhaite définir.
         * Ce mot de passe a déjà été validé par la couche présentation selon les règles
         * de validation spécifiées dans la classe ChangePasswordForm.
         */
        String newPassword
) {
    /**
     * Crée une nouvelle instance de PasswordChangeRequest avec validation.
     *
     * @param currentPassword Le mot de passe actuel
     * @param newPassword Le nouveau mot de passe
     * @throws IllegalArgumentException si un des arguments est null ou vide
     */
    public PasswordChangeRequest {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new PasswordValidationException("Le mot de passe actuel ne peut pas être vide");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new PasswordValidationException("Le nouveau mot de passe ne peut pas être vide");
        }
    }

    /**
     * Méthode utilitaire permettant de créer une instance à partir du formulaire de la couche présentation.
     *
     * @param form Formulaire de changement de mot de passe provenant de la couche présentation
     * @return Une nouvelle instance de PasswordChangeRequest
     */
    public static PasswordChangeRequest fromForm(ChangePasswordForm form) {
        return new PasswordChangeRequest(form.currentPassword(), form.password());
    }
}