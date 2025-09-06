package be.steby.CoreProject.bll.common.models.user;

import be.steby.CoreProject.dl.entities.User;

public record UserCreationRequest(
        User user,                          // L'utilisateur à créer
        String password,                    // null = génération automatique
        UserCreationMode mode         // Mode de création
) {
    /**
     *  Factory method for self-signup
     */

    public static UserCreationRequest forSelfSignup(User user, String password){
        return new UserCreationRequest(user, password, UserCreationMode.SELF_SIGNUP);
    }

    /**
     * Factory method pour admin creation
     */
    public static UserCreationRequest forAdminCreate(User user) {
        return new UserCreationRequest(user, null, UserCreationMode.ADMIN_CREATE);
    }

    /**
     * Factory method pour system creation
     */
    public static UserCreationRequest forSystemCreate(User user) {
        return new UserCreationRequest(user, null, UserCreationMode.SYSTEM_CREATE);
    }
}

