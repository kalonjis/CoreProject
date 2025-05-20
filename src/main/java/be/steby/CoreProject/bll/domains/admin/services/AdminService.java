package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {

    User createUser(User user);

    void deleteUser(Long id);

    void activateUser(Long id);

    void deactivateUser(Long id);

    void grantUserRole(Long id, UserRole role);

    void revokeUserRole(Long id, UserRole role);

    Page<User> searchUsers(String query, Pageable pageable);

    Page<User> searchUsersByCriteria(String username, String firstname, String lastname, String email, String phoneNumber, Pageable pageable);


    User getUserById(Long id);

    void triggerPasswordReset(Long id);

    List<Device> getUserDevices(Long id);

    Long getTotalUsers();
}
