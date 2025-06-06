package be.steby.CoreProject.bll.common.models.user;

import be.steby.CoreProject.dl.entities.User;


/**
 * Résultat de la création d'un utilisateur
 */
public record UserCreationResult(
    User user,
    String temporaryPassword,
    String confirmationToken
) {
    /**
     * Indique si un mot de passe temporaire a été généré
     */
    public boolean hasTemporaryPassword(){
        return temporaryPassword != null && !temporaryPassword.isBlank();
    }
}
