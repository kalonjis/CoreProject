package be.steby.CoreProject.dal;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {

        //region user
        User user1 = new User(
                "Gunt",
                "Gunter",
                "Doofenshmirtz",
                "leader@gmail.com",
                "0417/89 62 32",
                passwordEncoder.encode("GrosseBertha"),
                UserRole.setRoles(UserRole.SUPER_ADMIN)
        );
        User user2 = new User(
                "Souf",
                "Soufiane",
                "ScrumMaster",
                "souf@fake.com",
                "0498/56 78 90",
                passwordEncoder.encode("test123"),
                UserRole.setRoles(UserRole.ADMIN)
        );

        User user3 = new User(
                "Quent",
                "Quentin",
                "Wakabayashi",
                "quentin@fake.com",
                "0467/45 12 34",
                passwordEncoder.encode("test123"),
                UserRole.setRoles(UserRole.MODERATOR)
        );

        User user4 = new User(
                "Hongo",
                "Mauritcio",
                "Hongo",
                "hongo@fake.com",
                "0467/45 12 34",
                passwordEncoder.encode("test123"),
                UserRole.setRoles(UserRole.USER)
        );

        User user5 = new User(
                "Ben",
                "Benjamin",
                "En Short",
                "benja@fake.com",
                "0467/45 12 34",
                passwordEncoder.encode("test123"),
                UserRole.setRoles(UserRole.GUEST)
        );
        List<User> users = List.of(user1, user2, user3, user4, user5);
        users.forEach(
                u -> {
                    u.setMustChangePassword(false);
                    u.setEnabled(true);
                }
        );
        userRepository.saveAll(users);
        //endregion

    }
}
