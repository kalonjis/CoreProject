package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

public interface SecurityService {

    User getAuthenticatedUser();

    boolean isAnonymous();

    boolean authenticatedHasRole(UserRole role);
}
