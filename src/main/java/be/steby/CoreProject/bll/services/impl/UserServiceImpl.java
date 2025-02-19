package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.specifications.UserSpecification;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
                .orElseThrow(()-> new DoesntExistException("Username with id "+ id +" does not exist"));
    }
}

