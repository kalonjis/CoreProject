package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.exceptions.AlreadyExistException;
import be.steby.CoreProject.bll.exceptions.DoesntExistException;
import be.steby.CoreProject.bll.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailerService mailerService;


    /**
     * Creation of user and saved in db
     *
     * @param user
     * @return
     */
    @Override
    public User register(User user) {
        if ( userRepository.existsByUsernameIgnoreCase( user.getUsername() ) ) {
            throw new AlreadyExistException("User account with username : "+ user.getUsername() +" already exist");
        }
        if ( userRepository.existsByEmailIgnoreCase( user.getEmail() ) ) {
            throw new AlreadyExistException("User account with email : "+ user.getEmail() +" already exist");
        }
//        if ("ADMIN".equals(currentUser.getUserRole()) && "SUPER_ADMIN".equals(user.getUserRole())) {
//            throw new NotEnoughAuthorities("Not enough authorities");
//        }

        int temporaryPasswordNumbers = generateRandomNumber();
        String temporaryPassword = "W3lcome" +
                user.getFirstname().substring(0,1).toUpperCase() +
                "."+user.getLastname().substring(0,1).toUpperCase() +
                temporaryPasswordNumbers;
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        mailerService.sendAccountConfirmation(user, temporaryPassword);
        return user;
    }


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
    public void resetPassword(User user, String password) {

    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {

    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new DoesntExistException("User account with username : "+ username +" not Found: "));
    }


    private int generateRandomNumber() {
        return ThreadLocalRandom.current().nextInt(10000, 100000);
    }
}

