package be.steby.CoreProject.bll.common.services.user;

import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.models.user.UserValidationResult;
import be.steby.CoreProject.dl.entities.User;

public interface UserCreationService {

    /**
     * Crée un utilisateur avec validation complète
     *
     * @param request Paramètres de création utilisateur
     * @return Résultat de la création avec token et password temporaire si applicable
     */
    UserCreationResult createUser( UserCreationRequest request);

    /**
     * Valide les données utilisateur (email, password, roles)
     *
     * @param user L'utilisateur à valider
     * @param password Le mot de passe (peut être null pour génération auto)
     * @return Résultat de validation avec erreurs éventuelles
     */
    UserValidationResult validateUserData(User user, String password);

}
