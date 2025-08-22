package be.steby.CoreProject.bll.domains.emailAddress.models;

import be.steby.CoreProject.bll.domains.emailAddress.exceptions.EmailValidationException;
import be.steby.CoreProject.pl.models.emailAddress.ChangeEmailForm;

/**
 * Représente une demande de changement d'adresse email dans le domaine métier.
 * Ce DTO est utilisé par la couche service et ne contient pas d'annotations
 * de validation spécifiques à la couche présentation.
 */
public record EmailChangeRequest(
        /**
         * Nouvelle adresse email demandée par l'utilisateur.
         */
        String newEmail,

        /**
         * Confirmation de la nouvelle adresse email.
         * Doit être identique à newEmail.
         */
        String confirmEmail
) {
    /**
     * Crée une nouvelle instance d'EmailChangeRequest avec validation.
     *
     * Constructeur canonique compact avec validation.
     * Les paramètres (newEmail, confirmEmail) sont implicites dans un record.
     *
     * @throws EmailValidationException si les emails ne correspondent pas ou sont invalides
     */
    public EmailChangeRequest {
        if (newEmail == null || newEmail.isBlank()) {
            throw new EmailValidationException("La nouvelle adresse email ne peut pas être vide");
        }
        if (confirmEmail == null || confirmEmail.isBlank()) {
            throw new EmailValidationException("La confirmation de l'adresse email ne peut pas être vide");
        }
        if (!newEmail.equals(confirmEmail)) {
            throw new EmailValidationException("Les adresses email doivent être identiques");
        }
    }

    /**
     * Méthode utilitaire permettant de créer une instance à partir du formulaire de la couche présentation.
     *
     * @param form Formulaire de changement d'email provenant de la couche présentation
     * @return Une nouvelle instance d'EmailChangeRequest
     */
    public static EmailChangeRequest fromForm(ChangeEmailForm form) {
        return new EmailChangeRequest(form.email(), form.confirmEmail());
    }
}