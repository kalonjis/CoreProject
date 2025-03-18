package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityServiceImpl implements SecurityService {


    @Override
    public User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        } else {
            throw new UserAuthenticationStateException("No user connected", 401);
        }
    }


    @Override
    public boolean isAnonymous() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof AnonymousAuthenticationToken;
    }


    @Override
    public boolean authenticatedHasRole(UserRole role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role.name()));
    }
}
