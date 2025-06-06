package be.steby.CoreProject.dal;

import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
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
                "GrosseBertha",
                UserRole.setRoles(UserRole.SUPER_ADMIN)
        );
        User user2 = new User(
                "steby",
                "Soufiane",
                "ScrumMaster",
                "kalonj1981@hotmail.com",
                "0498/56 78 90",
                "test123",
                UserRole.setRoles(UserRole.ADMIN)
        );

        User user3 = new User(
                "Quent",
                "Quentin",
                "Wakabayashi",
                "quentin@fake.com",
                "0467/45 12 34",
                "test123",
                UserRole.setRoles(UserRole.MODERATOR)
        );

        User user4 = new User(
                "Hongo",
                "Mauritcio",
                "Hongo",
                "hongo@fake.com",
                "0467/45 12 34",
                "test123",
                UserRole.setRoles(UserRole.USER)
        );

        User user5 = new User(
                "Ben",
                "Benjamin",
                "En Short",
                "benja@fake.com",
                "0467/45 12 34",
                "test123",
                UserRole.setRoles(UserRole.GUEST)
        );
        List<User> users = List.of(user1, user2, user3, user4, user5);
        
        users.forEach(
                u -> {
                    u.setMustChangePassword(false);
                    u.setEnabled(true);
                    u.setEverActivated(true);
                }
        );
        userRepository.saveAll(users);
        //endregion


        //region devices

        // Gunt's devices
        Device device1 = Device.builder()
                .user(user1)
                .deviceType("MOBILE")
                .browser("Chrome")
                .operatingSystem("Android")
                .fingerprint("gunt_mobile_abc123")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.10")
                .deviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
                .build();

        Device device4 = Device.builder()
                .user(user1)
                .deviceType("LAPTOP")
                .browser("Edge")
                .operatingSystem("Windows")
                .fingerprint("gunt_laptop_xyz789")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.11")
                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
                .build();

        Device device5 = Device.builder()
                .user(user1)
                .deviceType("SMARTTV")
                .browser("SmartTV OS")
                .operatingSystem("SmartTV OS")
                .fingerprint("gunt_smarttv_qwe456")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.12")
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .build();

        // Steby's devices
        Device device2 = Device.builder()
                .user(user2)
                .deviceType("LAPTOP")
                .browser("Firefox")
                .operatingSystem("Windows")
                .fingerprint("steby_laptop_def456")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.20")
                .deviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
                .confirmed(true)
                .blacklisted(false)
                .build();

        Device device6 = Device.builder()
                .user(user2)
                .deviceType("MOBILE")
                .browser("Safari")
                .operatingSystem("iOS")
                .fingerprint("steby_mobile_rty123")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.21")
                .deviceTrustLevel(DeviceTrustLevel.BASIC)
                .confirmed(false)
                .blacklisted(false)
                .build();

        Device device7 = Device.builder()
                .user(user2)
                .deviceType("TABLET")
                .browser("Chrome")
                .operatingSystem("Android")
                .fingerprint("steby_tablet_uio789")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.22")
                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
                .confirmed(true)
                .blacklisted(false)
                .build();

        // Quent's devices
        Device device3 = Device.builder()
                .user(user3)
                .deviceType("TABLET")
                .browser("Safari")
                .operatingSystem("iOS")
                .fingerprint("quent_tablet_ghi789")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.30")
                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
                .confirmed(true)
                .blacklisted(false)
                .build();

        Device device8 = Device.builder()
                .user(user3)
                .deviceType("DESKTOP")
                .browser("Edge")
                .operatingSystem("Linux")
                .fingerprint("quent_desktop_asd456")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.31")
                .deviceTrustLevel(DeviceTrustLevel.BASIC)
                .confirmed(true)
                .blacklisted(false)
                .build();

        Device device9 = Device.builder()
                .user(user3)
                .deviceType("MOBILE")
                .browser("Firefox")
                .operatingSystem("Android")
                .fingerprint("quent_mobile_zxc123")
                .firstSeen(Instant.now())
                .lastSeen(Instant.now())
                .lastIpAddress("192.168.1.32")
                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
                .confirmed(false)
                .blacklisted(false)
                .build();

        deviceRepository.saveAll(List.of(device1, device2, device3, device4, device5, device6, device7, device8, device9));

        //endregion

    }
}
