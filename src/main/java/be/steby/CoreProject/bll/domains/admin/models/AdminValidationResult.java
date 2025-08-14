package be.steby.CoreProject.bll.domains.admin.models;

import java.util.List;

/**
 * Encapsule les résultats de validation pour les opérations administratives.
 * Suit le pattern établi par PasswordValidationResult et EmailValidationResult.
 */
public record AdminValidationResult(
        /**
         * Indique si la validation a réussi
         */
        boolean isValid,

        /**
         * Liste des erreurs de validation (vide si isValid = true)
         */
        List<String> errors
) {
    /**
     * Crée un résultat de validation réussi.
     */
    public static AdminValidationResult valid() {
        return new AdminValidationResult(true, List.of());
    }

    /**
     * Crée un résultat de validation échoué avec une seule erreur.
     */
    public static AdminValidationResult invalid(String error) {
        return new AdminValidationResult(false, List.of(error));
    }

    /**
     * Crée un résultat de validation échoué avec plusieurs erreurs.
     */
    public static AdminValidationResult invalid(List<String> errors) {
        return new AdminValidationResult(false, List.copyOf(errors));
    }

    /**
     * Vérifie s'il y a des erreurs de validation.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Retourne le nombre d'erreurs.
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * Retourne la première erreur ou null si aucune erreur.
     */
    public String firstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}