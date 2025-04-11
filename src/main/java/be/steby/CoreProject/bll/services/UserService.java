package be.steby.CoreProject.bll.services;


import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public interface UserService {

    Page<User> searchUsers(String query, Pageable pageable);

    Page<User> searchUsersByCriteria(String username, String firstname, String lastname, String email, String phoneNumber, Pageable pageable);

    User getUserById(Long id);

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    void saveUser(User user);

    void deleteUser(Long id);

    void activateUser(Long id);

    void deactivateUser(Long id);

    void setUserMailVerified(User user);

    void checkIfUserExists(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void grantUserRole(Long id, UserRole role);

    void revokeUserRole(Long id, UserRole role);

    Long getTotalUsers();
}

