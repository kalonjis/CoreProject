package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.EmailAlreadyTakenException;
import be.steby.CoreProject.bll.exceptions.UsernameAlreadyTakenException;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.specifications.UserSpecification;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;


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
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        if(!user.isEnabled()){
            throw new AttributeUnchangedException("The user is already deactivated.");
        }
        user.setEnabled(false);
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
            throw new EmailAlreadyTakenException("User account with email address: " + user.getEmail() + " already exists");
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


}

