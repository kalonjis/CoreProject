package be.steby.CoreProject.bll.common.models.user;


/**
 * Mode de création d'un utilisateur
 */
public enum UserCreationMode {
    SELF_SIGNUP,        // Auto-inscription par l'utilisateur
    ADMIN_CREATE,       // Création par un administrateur
    SYSTEM_CREATE       // Création système (migration, import, etc.)
}
