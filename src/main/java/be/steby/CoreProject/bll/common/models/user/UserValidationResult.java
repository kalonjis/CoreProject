package be.steby.CoreProject.bll.common.models.user;

import java.util.List;


/**
 * Résultat de validation d'un utilisateur
 */
public record UserValidationResult(
    boolean isValid,
    List<String> errors
) {

    /**
     * Validation réussie
     */
    public static UserValidationResult valid(){
        return new UserValidationResult(true, List.of());
    }

    /**
     * Validation échouée avec erreur unique
     */
    public static UserValidationResult invalid(String error) {
        return new UserValidationResult(false, List.of(error));
    }

    /**
     * Validation échouée avec erreurs multiples
     */
    public static UserValidationResult invalid(List<String> errors) {
        return new UserValidationResult(false, errors);
    }
}
