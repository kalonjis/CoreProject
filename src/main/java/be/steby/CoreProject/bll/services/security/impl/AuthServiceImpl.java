package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public User login(String username, String password) {
        User user = (User) loadUserByUsername(username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException("Incorrect password", 401);
        }
        return user;
    }

    @Override
    public void logout() {

    }

    @Override
    public User register(User user, User currentUser) {
        return null;
    }

    @Override
    public void resetPassword(User user, String password) {

    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {

    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new DoesntExistException("User account with username : "+ username +" not Found: "));
    }
}

