package be.steby.CoreProject.bll.common.models.user;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

public record UserCreationRequest(
        User user,                          // L'utilisateur à créer
        String password,                    // null = génération automatique
        UserCreationMode mode,              // Mode de création
        RequestContext requestContext      // Contexte de la requête
) {
    /**
     *  Factory method for self-signup
     */

    public static UserCreationRequest forSelfSignup(User user, String password, RequestContext requestContext){
        return new UserCreationRequest(user, password, UserCreationMode.SELF_SIGNUP, requestContext);
    }

    /**
     * Factory method pour admin creation
     */
    public static UserCreationRequest forAdminCreate(User user, RequestContext requestContext) {
        return new UserCreationRequest(user, null, UserCreationMode.ADMIN_CREATE, requestContext);
    }

    /**
     * Factory method pour system creation
     */
    public static UserCreationRequest forSystemCreate(User user, RequestContext requestContext) {
        return new UserCreationRequest(user, null, UserCreationMode.SYSTEM_CREATE, requestContext);
    }
}

