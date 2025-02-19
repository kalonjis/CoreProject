package be.steby.CoreProject.bll.services;


import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public interface UserService {

    Page<User> searchUsers(String query, Pageable pageable);

    Page<User> searchUsersByCriteria(String username, String firstname, String lastname, String email, String phoneNumber, Pageable pageable);

    User getUserById(Long id);
}

