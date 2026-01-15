package be.steby.CoreProject.dal;

import be.steby.CoreProject.bll.common.models.user.UserCreationMode;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserCreationService userCreationService;
    private final AddressRepository addressRepository;
    private final UserAddressRepository userAddressRepository;


    @Override
    public void run(String... args) throws Exception {

//        //region user
//        User user1 = new User(
//                "Gunt",
//                "Gunter",
//                "Doofenshmirtz",
//                "fakeGunt@fake.com",
//                "0417/89 62 32",
//                "Test1234!",
//                UserRole.setRoles(UserRole.SUPER_ADMIN)
//        );
//        User user2 = new User(
//                "steby",
//                "steph",
//                "CTO",
//                "kalonj1981@hotmail.com",
//                "0498567890",
//                "Test1234!",
//                UserRole.setRoles(UserRole.ADMIN)
//        );
//
//        User user3 = new User(
//                "Quent",
//                "Quentin",
//                "Wakabayashi",
//                "quentin@fake.com",
//                "0467/45 12 34",
//                "Test1234!",
//                UserRole.setRoles(UserRole.MODERATOR)
//        );
//
//        User user4 = new User(
//                "Hongo",
//                "Mauritcio",
//                "Hongo",
//                "hongo@fake.com",
//                "0467/45 12 34",
//                "Test1234!",
//                UserRole.setRoles(UserRole.USER)
//        );
//
//        User user5 = new User(
//                "Ben",
//                "Benjamin",
//                "En Short",
//                "benja@fake.com",
//                "0467/45 12 34",
//                "Test1234!",
//                UserRole.setRoles(UserRole.GUEST)
//        );
//
//        log.info("🔧 Initialisation des données système");
//
//
//        List<User> users = List.of(user1, user2, user3, user4, user5);
//
//        users.forEach(
//                u -> {
//                    UserCreationRequest userCreationRequest = new UserCreationRequest(
//                            u,
//                            u.getPassword(),
//                            UserCreationMode.SYSTEM_CREATE
//                    );
//                    userCreationService.createUser(userCreationRequest);
//                }
//        );
//
//
//        users.get(1).setMustChangePassword(false);
//        users.get(1).setPhoneNumberVerified(true);
//
//
//        userRepository.saveAll(users);
//
//
//        //endregion
//
//
//        //region devices
//
//        // Gunt's devices
//        Device device1 = Device.builder()
//                .user(user1)
//                .deviceType("MOBILE")
//                .browser("Chrome")
//                .operatingSystem("Android")
//                .fingerprint("gunt_mobile_abc123")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.10")
//                .deviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
//                .build();
//
//        Device device4 = Device.builder()
//                .user(user1)
//                .deviceType("LAPTOP")
//                .browser("Edge")
//                .operatingSystem("Windows")
//                .fingerprint("gunt_laptop_xyz789")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.11")
//                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
//                .build();
//
//        Device device5 = Device.builder()
//                .user(user1)
//                .deviceType("SMARTTV")
//                .browser("SmartTV OS")
//                .operatingSystem("SmartTV OS")
//                .fingerprint("gunt_smarttv_qwe456")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.12")
//                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
//                .build();
//
//        // Steby's devices
//        Device device2 = Device.builder()
//                .user(user2)
//                .deviceType("LAPTOP")
//                .browser("Firefox")
//                .operatingSystem("Windows")
//                .fingerprint("steby_laptop_def456")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.20")
//                .deviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
//                .confirmed(true)
//                .blacklisted(false)
//                .build();
//
//        Device device6 = Device.builder()
//                .user(user2)
//                .deviceType("MOBILE")
//                .browser("Safari")
//                .operatingSystem("iOS")
//                .fingerprint("steby_mobile_rty123")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.21")
//                .deviceTrustLevel(DeviceTrustLevel.BASIC)
//                .confirmed(false)
//                .blacklisted(false)
//                .build();
//
//        Device device7 = Device.builder()
//                .user(user2)
//                .deviceType("TABLET")
//                .browser("Chrome")
//                .operatingSystem("Android")
//                .fingerprint("steby_tablet_uio789")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.22")
//                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
//                .confirmed(true)
//                .blacklisted(false)
//                .build();
//
//        // Quent's devices
//        Device device3 = Device.builder()
//                .user(user3)
//                .deviceType("TABLET")
//                .browser("Safari")
//                .operatingSystem("iOS")
//                .fingerprint("quent_tablet_ghi789")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.30")
//                .deviceTrustLevel(DeviceTrustLevel.TRUSTED)
//                .confirmed(true)
//                .blacklisted(false)
//                .build();
//
//        Device device8 = Device.builder()
//                .user(user3)
//                .deviceType("DESKTOP")
//                .browser("Edge")
//                .operatingSystem("Linux")
//                .fingerprint("quent_desktop_asd456")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.31")
//                .deviceTrustLevel(DeviceTrustLevel.BASIC)
//                .confirmed(true)
//                .blacklisted(false)
//                .build();
//
//        Device device9 = Device.builder()
//                .user(user3)
//                .deviceType("MOBILE")
//                .browser("Firefox")
//                .operatingSystem("Android")
//                .fingerprint("quent_mobile_zxc123")
//                .firstSeen(Instant.now())
//                .lastSeen(Instant.now())
//                .lastIpAddress("192.168.1.32")
//                .deviceTrustLevel(DeviceTrustLevel.UNTRUSTED)
//                .confirmed(false)
//                .blacklisted(false)
//                .build();
//
//        List<Device> devices = List.of(device1,device2, device3, device4, device5, device6, device7, device8, device9);
//
//        for (Device d : devices){
//            d.setLoggedOut(true);
//        }
//
//        deviceRepository.saveAll(devices);
//
//        //endregion
//
//
//        //region addresses
//
//                log.info("🏠 Initializing addresses...");
//
//        // ===============================
//        // 1. CREATE ADDRESS ENTITIES
//        // ===============================
//
//        // Address 1: Gunt's home (Brussels, Belgium)
//                Address addr1 = Address.builder()
//                        .streetNumber("42")
//                        .streetName("Avenue Louise")
//                        .complement("Apt 3B")
//                        .postalCode("1050")
//                        .city("Bruxelles")
//                        .stateProvince("Bruxelles-Capitale")
//                        .countryCode("BE")
//                        .latitude(50.8263)
//                        .longitude(4.3621)
//                        .validated(true)
//                        .validationSource("GOOGLE_MAPS")
//                        .formattedAddress("42 Avenue Louise, Apt 3B, 1050 Bruxelles, Belgium")
//                        .build();
//
//        // Address 2: Gunt's work (Doofenshmirtz Evil Inc.)
//                Address addr2 = Address.builder()
//                        .streetNumber("666")
//                        .streetName("Rue de l'Industrie")
//                        .postalCode("1000")
//                        .city("Bruxelles")
//                        .countryCode("BE")
//                        .latitude(50.8476)
//                        .longitude(4.3572)
//                        .validated(true)
//                        .validationSource("MANUAL")
//                        .formattedAddress("666 Rue de l'Industrie, 1000 Bruxelles, Belgium")
//                        .build();
//
//        // Address 3: Steby's home (Louvain-la-Neuve, Belgium)
//                Address addr3 = Address.builder()
//                        .streetNumber("15")
//                        .streetName("Place de l'Université")
//                        .postalCode("1348")
//                        .city("Louvain-la-Neuve")
//                        .stateProvince("Brabant Wallon")
//                        .countryCode("BE")
//                        .latitude(50.6692)
//                        .longitude(4.6118)
//                        .validated(true)
//                        .validationSource("GOOGLE_MAPS")
//                        .formattedAddress("15 Place de l'Université, 1348 Louvain-la-Neuve, Belgium")
//                        .build();
//
//        // Address 4: Steby's billing address (different for billing purposes)
//                Address addr4 = Address.builder()
//                        .streetNumber("8")
//                        .streetName("Rue des Wallons")
//                        .postalCode("1348")
//                        .city("Louvain-la-Neuve")
//                        .countryCode("BE")
//                        .validated(false)
//                        .build();
//
//        // Address 5: Shared apartment (Steby & Quent - roommates)
//                Address addr5 = Address.builder()
//                        .streetNumber("23")
//                        .streetName("Avenue des Sports")
//                        .complement("Building A")
//                        .postalCode("1348")
//                        .city("Louvain-la-Neuve")
//                        .countryCode("BE")
//                        .latitude(50.6685)
//                        .longitude(4.6142)
//                        .validated(true)
//                        .validationSource("GOOGLE_MAPS")
//                        .formattedAddress("23 Avenue des Sports, Building A, 1348 Louvain-la-Neuve, Belgium")
//                        .build();
//
//        // Address 6: Quent's work address (Tokyo, Japan)
//                Address addr6 = Address.builder()
//                        .streetNumber("1-1-1")
//                        .streetName("Shibuya")
//                        .postalCode("150-0002")
//                        .city("Tokyo")
//                        .stateProvince("Tokyo")
//                        .countryCode("JP")
//                        .latitude(35.6595)
//                        .longitude(139.7004)
//                        .validated(true)
//                        .validationSource("GOOGLE_MAPS")
//                        .formattedAddress("1-1-1 Shibuya, Tokyo 150-0002, Japan")
//                        .build();
//
//        // Address 7: Hongo's home (Paris, France)
//                Address addr7 = Address.builder()
//                        .streetNumber("10")
//                        .streetName("Rue de Rivoli")
//                        .postalCode("75001")
//                        .city("Paris")
//                        .stateProvince("Île-de-France")
//                        .countryCode("FR")
//                        .latitude(48.8584)
//                        .longitude(2.3558)
//                        .validated(true)
//                        .validationSource("GOOGLE_MAPS")
//                        .formattedAddress("10 Rue de Rivoli, 75001 Paris, France")
//                        .build();
//
//        // Address 8: Hongo's work (La Défense)
//                Address addr8 = Address.builder()
//                        .streetNumber("5")
//                        .streetName("Esplanade du Général de Gaulle")
//                        .postalCode("92800")
//                        .city("Puteaux")
//                        .stateProvince("Île-de-France")
//                        .countryCode("FR")
//                        .validated(false)
//                        .build();
//
//        // Address 9: Ben's home (Namur, Belgium)
//                Address addr9 = Address.builder()
//                        .streetNumber("7")
//                        .streetName("Rue de Fer")
//                        .postalCode("5000")
//                        .city("Namur")
//                        .stateProvince("Namur")
//                        .countryCode("BE")
//                        .latitude(50.4669)
//                        .longitude(4.8672)
//                        .validated(true)
//                        .validationSource("MANUAL")
//                        .formattedAddress("7 Rue de Fer, 5000 Namur, Belgium")
//                        .build();
//
//        // Address 10: Old address (inactive - Steby moved out)
//                Address addr10 = Address.builder()
//                        .streetNumber("99")
//                        .streetName("Rue du Vieux Marché")
//                        .postalCode("1000")
//                        .city("Bruxelles")
//                        .countryCode("BE")
//                        .validated(true)
//                        .build();
//
//        // Save all addresses
//                List<Address> addresses = List.of(addr1, addr2, addr3, addr4, addr5, addr6, addr7, addr8, addr9, addr10);
//                addressRepository.saveAll(addresses);
//                log.info("✅ Created {} addresses", addresses.size());
//
//        // ===============================
//        // 2. CREATE USER-ADDRESS LINKS
//        // ===============================
//
//        // Gunt's addresses
//                UserAddress guntHome = new UserAddress(
//                        user1, addr1, AddressType.RESIDENTIAL, "Home"
//                );
//                guntHome.setDefault(true);
//                guntHome.setPrimary(true);
//                guntHome.setBillingEligible(true);
//                guntHome.setShippingEligible(true);
//                guntHome.setActive(true);
//                guntHome.setVerifiedByOwner(true);
//
//                UserAddress guntWork = new UserAddress(
//                        user1, addr2, AddressType.PROFESSIONAL, "Evil Inc."
//                );
//                guntWork.setDefault(false);
//                guntWork.setPrimary(false);
//                guntWork.setBillingEligible(false);
//                guntWork.setShippingEligible(false);
//                guntWork.setActive(true);
//                guntWork.setVerifiedByOwner(true);
//
//        // Steby's addresses
//                UserAddress stebyHome = new UserAddress(
//                        user2, addr3, AddressType.RESIDENTIAL, "Primary Home"
//                );
//                stebyHome.setDefault(true);
//                stebyHome.setPrimary(true);
//                stebyHome.setBillingEligible(true);
//                stebyHome.setShippingEligible(true);
//                stebyHome.setActive(true);
//                stebyHome.setVerifiedByOwner(true);
//
//                UserAddress stebyBilling = new UserAddress(
//                        user2, addr4, AddressType.BILLING, "Company Billing"
//                );
//                stebyBilling.setDefault(true); // default for BILLING type
//                stebyBilling.setPrimary(false);
//                stebyBilling.setBillingEligible(true);
//                stebyBilling.setShippingEligible(false);
//                stebyBilling.setActive(true);
//                stebyBilling.setVerifiedByOwner(false); // Not verified yet
//
//                UserAddress stebyShared = new UserAddress(
//                        user2, addr5, AddressType.SHIPPING, "Shared Apartment"
//                );
//                stebyShared.setDefault(false);
//                stebyShared.setPrimary(false);
//                stebyShared.setBillingEligible(false);
//                stebyShared.setShippingEligible(true);
//                stebyShared.setActive(true);
//                stebyShared.setVerifiedByOwner(true);
//
//                UserAddress stebyOld = new UserAddress(
//                        user2, addr10, AddressType.RESIDENTIAL, "Old Place"
//                );
//                stebyOld.setDefault(false);
//                stebyOld.setPrimary(false);
//                stebyOld.setActive(false); // Inactive - moved out
//                stebyOld.setVerifiedByOwner(true);
//
//        // Quent's addresses (shares addr5 with Steby)
//                UserAddress quentShared = new UserAddress(
//                        user3, addr5, AddressType.RESIDENTIAL, "Roommate Apartment"
//                );
//                quentShared.setDefault(true);
//                quentShared.setPrimary(true);
//                quentShared.setBillingEligible(true);
//                quentShared.setShippingEligible(true);
//                quentShared.setActive(true);
//                quentShared.setVerifiedByOwner(true);
//                quentShared.setNotes("Shared with Steby");
//
//                UserAddress quentWork = new UserAddress(
//                        user3, addr6, AddressType.PROFESSIONAL, "Tokyo Office"
//                );
//                quentWork.setDefault(false);
//                quentWork.setPrimary(false);
//                quentWork.setBillingEligible(false);
//                quentWork.setShippingEligible(true);
//                quentWork.setActive(true);
//                quentWork.setVerifiedByOwner(true);
//
//        // Hongo's addresses
//                UserAddress hongoHome = new UserAddress(
//                        user4, addr7, AddressType.RESIDENTIAL, "Paris Apartment"
//                );
//                hongoHome.setDefault(true);
//                hongoHome.setPrimary(true);
//                hongoHome.setBillingEligible(true);
//                hongoHome.setShippingEligible(true);
//                hongoHome.setActive(true);
//                hongoHome.setVerifiedByOwner(true);
//
//                UserAddress hongoWork = new UserAddress(
//                        user4, addr8, AddressType.PROFESSIONAL, "Office"
//                );
//                hongoWork.setDefault(false);
//                hongoWork.setPrimary(false);
//                hongoWork.setBillingEligible(false);
//                hongoWork.setShippingEligible(false);
//                hongoWork.setActive(true);
//                hongoWork.setVerifiedByOwner(false); // Not verified
//
//        // Ben's address
//                UserAddress benHome = new UserAddress(
//                        user5, addr9, AddressType.RESIDENTIAL, "Home"
//                );
//                benHome.setDefault(true);
//                benHome.setPrimary(true);
//                benHome.setBillingEligible(true);
//                benHome.setShippingEligible(true);
//                benHome.setActive(true);
//                benHome.setVerifiedByOwner(true);
//
//        // Save all user address links
//                List<UserAddress> userAddresses = List.of(
//                        guntHome, guntWork,
//                        stebyHome, stebyBilling, stebyShared, stebyOld,
//                        quentShared, quentWork,
//                        hongoHome, hongoWork,
//                        benHome
//                );
//                userAddressRepository.saveAll(userAddresses);
//                log.info("✅ Created {} user-address links", userAddresses.size());
//                log.info("🎉 Address initialization complete!");
//
//        //endregion
//
//            }
//
//
//        // Méthode helper pour extraire la version majeure proprement
//        private String extractMajorVersion(String osVersion) {
//            if (osVersion == null || osVersion.isEmpty()) {
//                return "Unknown";
//            }
//            try {
//                return osVersion.split("\\.")[0];
//            } catch (Exception e) {
//                return osVersion; // Retourner la version complète si on ne peut pas extraire
//            }
        }

}
