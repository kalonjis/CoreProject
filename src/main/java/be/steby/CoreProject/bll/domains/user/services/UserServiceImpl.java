package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.domains.emailAddress.exceptions.EmailAlreadyUsedException;
import be.steby.CoreProject.bll.exceptions.*;
import be.steby.CoreProject.bll.specifications.UserSpecification;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ReactivationType;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;



    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        log.info("Recherche globale avec query: '{}' et pagination: {}", query, pageable);
        Page<User> results = userRepository.findAll(UserSpecification.searchInAllFields(query), pageable);
        return results;
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname, String email, String phoneNumber, Pageable pageable) {
        log.info("Recherche par critères : username='{}', firstname='{}', lastname='{}', email='{}', phoneNumber='{}' avec pagination: {}",
                username, firstname, lastname, email, phoneNumber, pageable);

        Page<User> results = userRepository.findAll(UserSpecification.searchByCriteria(username, firstname, lastname, email, phoneNumber), pageable);
        return results;
    }

    @Override
    public User getUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(()-> new DoesntExistException("User with id "+ id +" does not exist"));
    }


    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new DoesntExistException("User account with username : "+ username +" not Found: "));
    }


    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(()-> new DoesntExistException("User with email address "+ email +" not found"));
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Override
    public void activateUser(Long id) {
        User user = getUserById(id);
        if(user.isEnabled()){
            throw new AttributeUnchangedException("The user is already activated.");
        }

        user.setEnabled(true);
        if( !user.isEverActivated() ){
            user.setEverActivated(true);
        }
        user.setActivatedAt(Instant.now());
        userRepository.save(user);
    }


    @Override
    public void reactivateUser(User user) {
        if(user.isEnabled()){
            throw new AttributeUnchangedException("The user is already activated.");
        }

        User authenticatedUser = getAuthenticatedUser();
        ReactivationType reactivationType;

        if (user.getId().equals(authenticatedUser.getId())) {
            // Auto-réactivation
            reactivationType = ReactivationType.SELF_REACTIVATION;
            user.setReactivatedBy(user);
        } else {
            // Réactivation par admin - utilise la méthode helper
            reactivationType = determineAdminReactivationType(user);
            user.setReactivatedBy(authenticatedUser);
        }

        user.setEnabled(true);
        user.setReactivatedAt(Instant.now());
        user.setReactivationType(reactivationType);
        userRepository.save(user);
    }


    public void adminActivateUser(User target, User admin) {
        if(target.isEnabled()){
            throw new AttributeUnchangedException("The user is already activated.");
        }

        if( target.getUserRoles().contains(UserRole.SUPER_ADMIN)
                && !authenticatedHasRole(UserRole.SUPER_ADMIN) ) {
            throw UserPermissionExceptionFactory.forSuperAdminAction("activer");
        }

        // ✅ LOGIQUE SIMPLE POUR ADMIN RÉACTIVATION - utilise la méthode helper
        ReactivationType reactivationType = determineAdminReactivationType(target);

        target.setEnabled(true);
        target.setReactivatedAt(Instant.now());
        target.setReactivatedBy(admin);
        target.setReactivationType(reactivationType);

        userRepository.save(target);
    }

    @Override
    public void deactivateUser(Long id, DeactivationReason reason, String reasonDetails ) {
        User user = getUserById(id);
        if(!user.isEnabled()){
            throw new AttributeUnchangedException("The user is already deactivated.");
        }

        if( user.getUserRoles().contains(UserRole.SUPER_ADMIN)
                && !authenticatedHasRole(UserRole.SUPER_ADMIN) ) {
            throw UserPermissionExceptionFactory.forSuperAdminAction("désactiver");
        }

        user.setEnabled(false);
        user.setDeactivatedAt(Instant.now());
        user.setDeactivationReason(reason);
        user.setDeactivationDetails(reasonDetails);
        userRepository.save(user);
    }

    public void adminDeactivateUser(Long id, AdminDeactivationCategory deactivationCategory, String adminDeactivationDetails){
        User targetUser = getUserById(id);
        if(!targetUser.isEnabled()){
            throw new AttributeUnchangedException("The user is already deactivated.");
        }

        User admin = getAuthenticatedUser();
        Set<UserRole> adminRoles = admin.getUserRoles();

        // ✅ NOUVELLE VÉRIFICATION DÉFENSIVE - Empêche l'auto-targeting
        if (admin.getId().equals(targetUser.getId())) {
            throw UserPermissionExceptionFactory.forAdminSelfTargeting();
        }

        // Vérification des permissions administratives
        if (!adminRoles.contains(UserRole.SUPER_ADMIN) && !adminRoles.contains(UserRole.ADMIN)){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("désactiver un utilisateur");
        }

        // Vérification spécifique SUPER_ADMIN
        if( targetUser.getUserRoles().contains(UserRole.SUPER_ADMIN)
                && !adminRoles.contains(UserRole.SUPER_ADMIN) ) {
            throw UserPermissionExceptionFactory.forSuperAdminAction("désactiver");
        }

        // ✅ NOUVELLE VÉRIFICATION - Validation selon les politiques
        // Si l'admin essaie de contourner les règles de self-deactivation
        if (admin.getId().equals(targetUser.getId()) &&
                !userPermissionService.canSelfDeactivate(admin)) {
            throw UserPermissionExceptionFactory.forSelfDeactivationPolicyBypass();
        }

        // Procéder à la désactivation
        targetUser.setEnabled(false);
        targetUser.setAdminDeactivationReason(deactivationCategory);
        targetUser.setAdminDeactivationDetails(adminDeactivationDetails);
        targetUser.setAdminDeactivatedBy(admin);
        targetUser.setAdminDeactivatedAt(Instant.now());

        userRepository.save(targetUser);

        // Log de l'action
        log.info("User {} administratively deactivated by admin {} with category: {}",
                targetUser.getUsername(), admin.getUsername(), deactivationCategory);
    }

    /**
     * @param user
     */
    @Override
    public void gdprUserDelete(User user) {

        // Anonymiser les données personnelles
        user.setEmail("deleted_" + user.getId() + "@anonymized.local");
        user.setFirstname("Utilisateur");
        user.setLastname("Supprimé");
        user.setPhoneNumber(null);

        // Désactiver le compte
        user.setEnabled(false);
        user.setDeactivatedAt(Instant.now());
        user.setDeactivationReason(DeactivationReason.GDPR_REQUEST);
        userRepository.save(user);
    }

    @Override
    public void setUserMailVerified(User user) {
        user.setEmailVerified(true);
        userRepository.save(user);
    }


    @Override
    public void checkIfUserExists(User user) {
        if ( existsByUsername(user.getUsername()) ) {
            throw new UsernameAlreadyTakenException("User account with username: " + user.getUsername() + " already exists");
        }
        if ( existsByEmail(user.getEmail()) ) {
            throw new EmailAlreadyUsedException("User account with email address: " + user.getEmail() + " already exists");
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }


    @Override
    public void grantUserRole(Long id, UserRole role) {
        User user = getUserById(id);
        if( user.getUserRoles().contains(role)){
            throw new AttributeUnchangedException("The user is already granted with role " + role);
        }

        user.getUserRoles().add(role);
        userRepository.save(user);
    }

    @Override
    public void revokeUserRole(Long id, UserRole role) {
        User user = getUserById(id);
        if(! user.getUserRoles().contains(role)){
            throw new AttributeUnchangedException("The user is not granted with role " + role);
        }

        user.getUserRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    public Long getTotalUsers() {
        return userRepository.count();
    }


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

    @Override
    public void requireAdminPermissions(){
        // ADMIN ou SUPER_ADMIN peuvent faire des opérations admin
        if( !authenticatedHasRole(UserRole.ADMIN) && !authenticatedHasRole(UserRole.SUPER_ADMIN) ){
            throw UserPermissionExceptionFactory.forAdminPermissionRequired("effectuer des opérations d'administration");
        }
    }

    // ✅ AJOUTER dans UserServiceImpl.java :
    @Override
    public void requireSuperAdminPermissions() {
        // Seuls les SUPER_ADMIN peuvent faire des opérations super admin
        if (!authenticatedHasRole(UserRole.SUPER_ADMIN)) {
            throw UserPermissionExceptionFactory.forInsufficientPermissions(UserRole.SUPER_ADMIN, "effectuer des opérations de super administration");
        }
    }

    private ReactivationType determineAdminReactivationType(User target) {
        if (target.getAdminDeactivationReason() != null &&
                target.getAdminDeactivationReason().requiresSuperAdminReactivation()) {
            return ReactivationType.SUPER_ADMIN_REACTIVATION;
        } else {
            return ReactivationType.ADMIN_REACTIVATION;
        }
    }

}