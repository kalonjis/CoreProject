package be.steby.CoreProject.dal;

import be.steby.CoreProject.bll.common.models.user.UserCreationMode;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealContactRoleRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.repositories.crm.PipelineRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.DealContactRole;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.entities.crm.Pipeline;
import be.steby.CoreProject.dl.entities.crm.PipelineStep;
import be.steby.CoreProject.dl.enums.crm.ContactRole;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.crm.ContactStatus;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    private final PipelineRepository pipelineRepository;
    private final OrganisationRepository organisationRepository;
    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final DealContactRoleRepository dealContactRoleRepository;


    @Override
    public void run(String... args) throws Exception {

        //region user
        User user1 = new User(
                "Gunt",
                "Gunter",
                "Doofenshmirtz",
                "fakeGunt@fake.com",
                "0417/89 62 32",
                "Test1234!",
                UserRole.setRoles(UserRole.ADMIN)
        );
        User user2 = new User(
                "steby",
                "steph",
                "CTO",
                "kalonj1981@hotmail.com",
                "0498567890",
                "Test1234!",
                UserRole.setRoles(UserRole.SUPER_ADMIN)
        );

        User user3 = new User(
                "Quent",
                "Quentin",
                "Wakabayashi",
                "quentin@fake.com",
                "0467/45 12 34",
                "Test1234!",
                UserRole.setRoles(UserRole.MODERATOR)
        );

        User user4 = new User(
                "Hongo",
                "Mauritcio",
                "Hongo",
                "hongo@fake.com",
                "0467/45 12 34",
                "Test1234!",
                UserRole.setRoles(UserRole.USER)
        );

        User user5 = new User(
                "Ben",
                "Benjamin",
                "En Short",
                "benja@fake.com",
                "0467/45 12 34",
                "Test1234!",
                UserRole.setRoles(UserRole.GUEST)
        );

        log.info("🔧 Initialisation des données système");

        boolean usersAlreadyExist = userRepository.existsByEmailIgnoreCase(user1.getEmail());

        if (!usersAlreadyExist) {

        List<User> users = List.of(user1, user2, user3, user4, user5);

        users.forEach(
                u -> {
                    UserCreationRequest userCreationRequest = new UserCreationRequest(
                            u,
                            u.getPassword(),
                            UserCreationMode.SYSTEM_CREATE
                    );
                    userCreationService.createUser(userCreationRequest);
                }
        );


        users.get(1).setMustChangePassword(false);
        users.get(1).setPhoneNumberVerified(true);
        users.get(1).getUserRoles().add(UserRole.MONITORING);



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

        List<Device> devices = List.of(device1,device2, device3, device4, device5, device6, device7, device8, device9);

        for (Device d : devices){
            d.setLoggedOut(true);
        }

        deviceRepository.saveAll(devices);

        //endregion


        //region addresses

                log.info("🏠 Initializing addresses...");

        // ===============================
        // 1. CREATE ADDRESS ENTITIES
        // ===============================

        // Address 1: Gunt's home (Brussels, Belgium)
                Address addr1 = Address.builder()
                        .streetNumber("42")
                        .streetName("Avenue Louise")
                        .complement("Apt 3B")
                        .postalCode("1050")
                        .city("Bruxelles")
                        .stateProvince("Bruxelles-Capitale")
                        .countryCode("BE")
                        .latitude(50.8263)
                        .longitude(4.3621)
                        .validated(true)
                        .validationSource("GOOGLE_MAPS")
                        .formattedAddress("42 Avenue Louise, Apt 3B, 1050 Bruxelles, Belgium")
                        .build();

        // Address 2: Gunt's work (Doofenshmirtz Evil Inc.)
                Address addr2 = Address.builder()
                        .streetNumber("666")
                        .streetName("Rue de l'Industrie")
                        .postalCode("1000")
                        .city("Bruxelles")
                        .countryCode("BE")
                        .latitude(50.8476)
                        .longitude(4.3572)
                        .validated(true)
                        .validationSource("MANUAL")
                        .formattedAddress("666 Rue de l'Industrie, 1000 Bruxelles, Belgium")
                        .build();

        // Address 3: Steby's home (Louvain-la-Neuve, Belgium)
                Address addr3 = Address.builder()
                        .streetNumber("15")
                        .streetName("Place de l'Université")
                        .postalCode("1348")
                        .city("Louvain-la-Neuve")
                        .stateProvince("Brabant Wallon")
                        .countryCode("BE")
                        .latitude(50.6692)
                        .longitude(4.6118)
                        .validated(true)
                        .validationSource("GOOGLE_MAPS")
                        .formattedAddress("15 Place de l'Université, 1348 Louvain-la-Neuve, Belgium")
                        .build();

        // Address 4: Steby's billing address (different for billing purposes)
                Address addr4 = Address.builder()
                        .streetNumber("8")
                        .streetName("Rue des Wallons")
                        .postalCode("1348")
                        .city("Louvain-la-Neuve")
                        .countryCode("BE")
                        .validated(false)
                        .build();

        // Address 5: Shared apartment (Steby & Quent - roommates)
                Address addr5 = Address.builder()
                        .streetNumber("23")
                        .streetName("Avenue des Sports")
                        .complement("Building A")
                        .postalCode("1348")
                        .city("Louvain-la-Neuve")
                        .countryCode("BE")
                        .latitude(50.6685)
                        .longitude(4.6142)
                        .validated(true)
                        .validationSource("GOOGLE_MAPS")
                        .formattedAddress("23 Avenue des Sports, Building A, 1348 Louvain-la-Neuve, Belgium")
                        .build();

        // Address 6: Quent's work address (Tokyo, Japan)
                Address addr6 = Address.builder()
                        .streetNumber("1-1-1")
                        .streetName("Shibuya")
                        .postalCode("150-0002")
                        .city("Tokyo")
                        .stateProvince("Tokyo")
                        .countryCode("JP")
                        .latitude(35.6595)
                        .longitude(139.7004)
                        .validated(true)
                        .validationSource("GOOGLE_MAPS")
                        .formattedAddress("1-1-1 Shibuya, Tokyo 150-0002, Japan")
                        .build();

        // Address 7: Hongo's home (Paris, France)
                Address addr7 = Address.builder()
                        .streetNumber("10")
                        .streetName("Rue de Rivoli")
                        .postalCode("75001")
                        .city("Paris")
                        .stateProvince("Île-de-France")
                        .countryCode("FR")
                        .latitude(48.8584)
                        .longitude(2.3558)
                        .validated(true)
                        .validationSource("GOOGLE_MAPS")
                        .formattedAddress("10 Rue de Rivoli, 75001 Paris, France")
                        .build();

        // Address 8: Hongo's work (La Défense)
                Address addr8 = Address.builder()
                        .streetNumber("5")
                        .streetName("Esplanade du Général de Gaulle")
                        .postalCode("92800")
                        .city("Puteaux")
                        .stateProvince("Île-de-France")
                        .countryCode("FR")
                        .validated(false)
                        .build();

        // Address 9: Ben's home (Namur, Belgium)
                Address addr9 = Address.builder()
                        .streetNumber("7")
                        .streetName("Rue de Fer")
                        .postalCode("5000")
                        .city("Namur")
                        .stateProvince("Namur")
                        .countryCode("BE")
                        .latitude(50.4669)
                        .longitude(4.8672)
                        .validated(true)
                        .validationSource("MANUAL")
                        .formattedAddress("7 Rue de Fer, 5000 Namur, Belgium")
                        .build();

        // Address 10: Old address (inactive - Steby moved out)
                Address addr10 = Address.builder()
                        .streetNumber("99")
                        .streetName("Rue du Vieux Marché")
                        .postalCode("1000")
                        .city("Bruxelles")
                        .countryCode("BE")
                        .validated(true)
                        .build();

        // Save all addresses
                List<Address> addresses = List.of(addr1, addr2, addr3, addr4, addr5, addr6, addr7, addr8, addr9, addr10);
                addressRepository.saveAll(addresses);
                log.info("✅ Created {} addresses", addresses.size());

        // ===============================
        // region CREATE USER-ADDRESS LINKS
        // ===============================

        // Gunt's addresses
                UserAddress guntHome = new UserAddress(
                        user1, addr1, AddressType.RESIDENTIAL, "Home"
                );
                guntHome.setDefault(true);
                guntHome.setPrimary(true);
                guntHome.setBillingEligible(true);
                guntHome.setShippingEligible(true);
                guntHome.setActive(true);
                guntHome.setVerifiedByOwner(true);

                UserAddress guntWork = new UserAddress(
                        user1, addr2, AddressType.PROFESSIONAL, "Evil Inc."
                );
                guntWork.setDefault(false);
                guntWork.setPrimary(false);
                guntWork.setBillingEligible(false);
                guntWork.setShippingEligible(false);
                guntWork.setActive(true);
                guntWork.setVerifiedByOwner(true);

        // Steby's addresses
                UserAddress stebyHome = new UserAddress(
                        user2, addr3, AddressType.RESIDENTIAL, "Primary Home"
                );
                stebyHome.setDefault(true);
                stebyHome.setPrimary(true);
                stebyHome.setBillingEligible(true);
                stebyHome.setShippingEligible(true);
                stebyHome.setActive(true);
                stebyHome.setVerifiedByOwner(true);

                UserAddress stebyBilling = new UserAddress(
                        user2, addr4, AddressType.BILLING, "Company Billing"
                );
                stebyBilling.setDefault(true); // default for BILLING type
                stebyBilling.setPrimary(false);
                stebyBilling.setBillingEligible(true);
                stebyBilling.setShippingEligible(false);
                stebyBilling.setActive(true);
                stebyBilling.setVerifiedByOwner(false); // Not verified yet

                UserAddress stebyShared = new UserAddress(
                        user2, addr5, AddressType.SHIPPING, "Shared Apartment"
                );
                stebyShared.setDefault(false);
                stebyShared.setPrimary(false);
                stebyShared.setBillingEligible(false);
                stebyShared.setShippingEligible(true);
                stebyShared.setActive(true);
                stebyShared.setVerifiedByOwner(true);

                UserAddress stebyOld = new UserAddress(
                        user2, addr10, AddressType.RESIDENTIAL, "Old Place"
                );
                stebyOld.setDefault(false);
                stebyOld.setPrimary(false);
                stebyOld.setActive(false); // Inactive - moved out
                stebyOld.setVerifiedByOwner(true);

        // Quent's addresses (shares addr5 with Steby)
                UserAddress quentShared = new UserAddress(
                        user3, addr5, AddressType.RESIDENTIAL, "Roommate Apartment"
                );
                quentShared.setDefault(true);
                quentShared.setPrimary(true);
                quentShared.setBillingEligible(true);
                quentShared.setShippingEligible(true);
                quentShared.setActive(true);
                quentShared.setVerifiedByOwner(true);
                quentShared.setNotes("Shared with Steby");

                UserAddress quentWork = new UserAddress(
                        user3, addr6, AddressType.PROFESSIONAL, "Tokyo Office"
                );
                quentWork.setDefault(false);
                quentWork.setPrimary(false);
                quentWork.setBillingEligible(false);
                quentWork.setShippingEligible(true);
                quentWork.setActive(true);
                quentWork.setVerifiedByOwner(true);

        // Hongo's addresses
                UserAddress hongoHome = new UserAddress(
                        user4, addr7, AddressType.RESIDENTIAL, "Paris Apartment"
                );
                hongoHome.setDefault(true);
                hongoHome.setPrimary(true);
                hongoHome.setBillingEligible(true);
                hongoHome.setShippingEligible(true);
                hongoHome.setActive(true);
                hongoHome.setVerifiedByOwner(true);

                UserAddress hongoWork = new UserAddress(
                        user4, addr8, AddressType.PROFESSIONAL, "Office"
                );
                hongoWork.setDefault(false);
                hongoWork.setPrimary(false);
                hongoWork.setBillingEligible(false);
                hongoWork.setShippingEligible(false);
                hongoWork.setActive(true);
                hongoWork.setVerifiedByOwner(false); // Not verified

        // Ben's address
                UserAddress benHome = new UserAddress(
                        user5, addr9, AddressType.RESIDENTIAL, "Home"
                );
                benHome.setDefault(true);
                benHome.setPrimary(true);
                benHome.setBillingEligible(true);
                benHome.setShippingEligible(true);
                benHome.setActive(true);
                benHome.setVerifiedByOwner(true);

        // Save all user address links
                List<UserAddress> userAddresses = List.of(
                        guntHome, guntWork,
                        stebyHome, stebyBilling, stebyShared, stebyOld,
                        quentShared, quentWork,
                        hongoHome, hongoWork,
                        benHome
                );
                userAddressRepository.saveAll(userAddresses);
                log.info("✅ Created {} user-address links", userAddresses.size());
                log.info("🎉 Address initialization complete!");

        } // end if (!usersAlreadyExist)

        //endregion


        //region CRM

        if (pipelineRepository.count() > 0) {
            log.info("CRM data already exists, skipping");
            return;
        }

        // Si les users existaient avant ce run, recharger depuis la DB
        if (usersAlreadyExist) {
            user1 = userRepository.findByEmailIgnoreCase("fakeGunt@fake.com").orElseThrow();
            user2 = userRepository.findByEmailIgnoreCase("kalonj1981@hotmail.com").orElseThrow();
        }

        log.info("🔧 Initializing CRM data...");

        // -----------------------------------------------------------------------
        // PIPELINE
        // -----------------------------------------------------------------------

        Pipeline pipeline = Pipeline.builder()
                .name("B2B Services")
                .description("Pipeline principal pour les contrats de services aux entreprises.")
                .isDefault(true)
                .displayOrder(0)
                .build();

        PipelineStep stepQualif = PipelineStep.builder()
                .name("Qualification")
                .color("#3498db")
                .position(0)
                .pipeline(pipeline)
                .build();

        PipelineStep stepDevis = PipelineStep.builder()
                .name("Devis envoyé")
                .color("#9b59b6")
                .position(1)
                .pipeline(pipeline)
                .build();

        PipelineStep stepNego = PipelineStep.builder()
                .name("Négociation")
                .color("#e67e22")
                .position(2)
                .pipeline(pipeline)
                .build();

        PipelineStep stepVisite = PipelineStep.builder()
                .name("Visite technique")
                .color("#1abc9c")
                .position(3)
                .pipeline(pipeline)
                .build();

        PipelineStep stepWon = PipelineStep.builder()
                .name("Gagné")
                .color("#27ae60")
                .position(4)
                .isWon(true)
                .pipeline(pipeline)
                .build();

        PipelineStep stepLost = PipelineStep.builder()
                .name("Perdu")
                .color("#e74c3c")
                .position(5)
                .isLost(true)
                .pipeline(pipeline)
                .build();

        pipeline.getPipelineSteps().addAll(List.of(stepQualif, stepDevis, stepNego, stepVisite, stepWon, stepLost));
        pipelineRepository.save(pipeline);
        log.info("✅ Pipeline '{}' created with {} steps", pipeline.getName(), pipeline.getPipelineSteps().size());

        // -----------------------------------------------------------------------
        // ORGANISATIONS
        // -----------------------------------------------------------------------

        Organisation orgAcme = Organisation.builder()
                .name("ACME Cleaning SA")
                .website("https://www.acme-cleaning.be")
                .industry("Nettoyage industriel")
                .size(OrganisationSize.MEDIUM)
                .phone("+32 2 456 78 90")
                .notes("Client historique depuis 2019. Contrat annuel reconductible.")
                .build();

        Organisation orgTechno = Organisation.builder()
                .name("TechnoPlus SPRL")
                .website("https://www.technoplus.be")
                .industry("Technologies de l'information")
                .size(OrganisationSize.SMALL)
                .phone("+32 4 789 01 23")
                .build();

        Organisation orgImmo = Organisation.builder()
                .name("Immo Prestige SA")
                .website("https://www.immoprestige.be")
                .industry("Immobilier")
                .size(OrganisationSize.LARGE)
                .phone("+32 2 111 22 33")
                .notes("Gestion de 50+ immeubles. Fort potentiel de contrats récurrents.")
                .build();

        Organisation orgSante = Organisation.builder()
                .name("Centre Médical du Parc")
                .industry("Santé")
                .size(OrganisationSize.SMALL)
                .phone("+32 81 44 55 66")
                .build();

        Organisation orgLogistique = Organisation.builder()
                .name("BelExpress Logistics")
                .website("https://www.belexpress.be")
                .industry("Logistique et transport")
                .size(OrganisationSize.ENTERPRISE)
                .phone("+32 3 987 65 43")
                .notes("Entrepôts à Liège et Anvers. Contrat multi-sites en discussion.")
                .build();

        organisationRepository.saveAll(List.of(orgAcme, orgTechno, orgImmo, orgSante, orgLogistique));
        log.info("✅ Created 5 organisations");

        // -----------------------------------------------------------------------
        // LEADS
        // -----------------------------------------------------------------------

        Lead lead1 = Lead.builder()
                .email("thomas.martin@acme-cleaning.be")
                .firstName("Thomas").lastName("Martin")
                .phone("+32 475 11 22 33")
                .subject("Demande de devis nettoyage bureaux")
                .leadType(LeadType.COMMERCIAL)
                .status(LeadStatus.CONVERTED)
                .assignedTo(user1)
                .submittedAt(Instant.now().minusSeconds(86400 * 30))
                .convertedAt(Instant.now().minusSeconds(86400 * 25))
                .ipAddress("85.200.1.50")
                .build();

        Lead lead2 = Lead.builder()
                .email("sophie.durand@technoplus.be")
                .firstName("Sophie").lastName("Durand")
                .subject("Partenariat technologique")
                .leadType(LeadType.PARTNERSHIP)
                .status(LeadStatus.IN_REVIEW)
                .assignedTo(user2)
                .submittedAt(Instant.now().minusSeconds(86400 * 10))
                .ipAddress("195.10.20.30")
                .build();

        Lead lead3 = Lead.builder()
                .email("marc.lecomte@immoprestige.be")
                .firstName("Marc").lastName("Lecomte")
                .phone("+32 474 55 66 77")
                .subject("Nettoyage résidences haut de gamme")
                .leadType(LeadType.COMMERCIAL)
                .status(LeadStatus.CONVERTED)
                .assignedTo(user1)
                .submittedAt(Instant.now().minusSeconds(86400 * 20))
                .convertedAt(Instant.now().minusSeconds(86400 * 15))
                .ipAddress("212.44.5.6")
                .build();

        Lead lead4 = Lead.builder()
                .email("press@dailybel.be")
                .firstName("Rédaction DailyBel")
                .subject("Interview fondateur")
                .leadType(LeadType.PRESS)
                .status(LeadStatus.REJECTED)
                .assignedTo(user2)
                .rejectionReason("Hors périmètre commercial — demande presse non prioritaire")
                .submittedAt(Instant.now().minusSeconds(86400 * 5))
                .ipAddress("195.50.100.1")
                .build();

        Lead lead5 = Lead.builder()
                .email("info@belexpress.be")
                .firstName("Service Achats BelExpress")
                .phone("+32 3 987 65 43")
                .subject("Contrat nettoyage entrepôts multi-sites")
                .leadType(LeadType.COMMERCIAL)
                .status(LeadStatus.CONVERTED)
                .assignedTo(user1)
                .submittedAt(Instant.now().minusSeconds(86400 * 45))
                .convertedAt(Instant.now().minusSeconds(86400 * 40))
                .ipAddress("91.200.30.10")
                .build();

        Lead lead6 = Lead.builder()
                .email("contact@clinique-parc.be")
                .firstName("Isabelle").lastName("Fontaine")
                .subject("Renseignements nettoyage médical")
                .leadType(LeadType.COMMERCIAL)
                .status(LeadStatus.NEW)
                .submittedAt(Instant.now().minusSeconds(86400 * 2))
                .ipAddress("178.51.22.44")
                .build();

        leadRepository.saveAll(List.of(lead1, lead2, lead3, lead4, lead5, lead6));
        log.info("✅ Created 6 leads");

        // -----------------------------------------------------------------------
        // CONTACTS
        // -----------------------------------------------------------------------

        Contact contactThomas = Contact.builder()
                .firstName("Thomas")
                .lastName("Martin")
                .email("thomas.martin@acme-cleaning.be")
                .phone("+32 475 11 22 33")
                .jobTitle("Directeur Général")
                .status(ContactStatus.QUALIFIED)
                .assignedTo(user1)
                .organisation(orgAcme)
                .originLead(lead1)
                .notes("Décideur principal. Appelle le vendredi matin.")
                .build();

        Contact contactSophie = Contact.builder()
                .firstName("Sophie")
                .lastName("Durand")
                .email("sophie.durand@technoplus.be")
                .phone("+32 476 22 33 44")
                .jobTitle("CTO")
                .status(ContactStatus.NEW)
                .assignedTo(user2)
                .organisation(orgTechno)
                .build();

        Contact contactMarc = Contact.builder()
                .firstName("Marc")
                .lastName("Lecomte")
                .email("marc.lecomte@immoprestige.be")
                .phone("+32 474 55 66 77")
                .jobTitle("Responsable Facility Management")
                .status(ContactStatus.QUALIFIED)
                .assignedTo(user1)
                .organisation(orgImmo)
                .originLead(lead3)
                .build();

        Contact contactIsabelle = Contact.builder()
                .firstName("Isabelle")
                .lastName("Fontaine")
                .email("isabelle.fontaine@clinique-parc.be")
                .phone("+32 81 44 55 66")
                .jobTitle("Directrice Médicale")
                .status(ContactStatus.NEW)
                .organisation(orgSante)
                .build();

        Contact contactPierre = Contact.builder()
                .firstName("Pierre")
                .lastName("Vanderberg")
                .email("pierre.vanderberg@belexpress.be")
                .phone("+32 477 88 99 00")
                .jobTitle("Directeur Achats")
                .status(ContactStatus.QUALIFIED)
                .assignedTo(user1)
                .organisation(orgLogistique)
                .originLead(lead5)
                .notes("Négocie dur mais loyal. Budget approuvé pour 3 sites.")
                .build();

        Contact contactNadia = Contact.builder()
                .firstName("Nadia")
                .lastName("Osman")
                .email("nadia.osman@freelance.be")
                .phone("+32 496 33 44 55")
                .jobTitle("Consultante Indépendante")
                .status(ContactStatus.NEW)
                .assignedTo(user2)
                .notes("Contact freelance, pas d'organisation liée.")
                .build();

        Contact contactJean = Contact.builder()
                .firstName("Jean-Pierre")
                .lastName("Dubois")
                .email("jpdubois@technoplus.be")
                .phone("+32 471 66 77 88")
                .jobTitle("CEO")
                .status(ContactStatus.CLIENT)
                .assignedTo(user2)
                .organisation(orgTechno)
                .build();

        contactRepository.saveAll(List.of(contactThomas, contactSophie, contactMarc,
                contactIsabelle, contactPierre, contactNadia, contactJean));
        log.info("✅ Created 7 contacts");

        // -----------------------------------------------------------------------
        // DEALS
        // -----------------------------------------------------------------------

        Deal deal1 = Deal.builder()
                .title("Nettoyage bureaux — ACME Cleaning SA")
                .amount(new BigDecimal("1200.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepDevis)
                .organisation(orgAcme)
                .assignedTo(user1)
                .expectedCloseDate(LocalDate.now().plusDays(15))
                .notes("Devis envoyé le 10/03. Attente retour client.")
                .build();

        Deal deal2 = Deal.builder()
                .title("Contrat entretien — Immo Prestige")
                .amount(new BigDecimal("3500.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepNego)
                .organisation(orgImmo)
                .assignedTo(user1)
                .expectedCloseDate(LocalDate.now().plusDays(30))
                .notes("Négociation sur le volume — 12 résidences.")
                .build();

        Deal deal3 = Deal.builder()
                .title("Multi-sites entrepôts — BelExpress")
                .amount(new BigDecimal("8900.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepVisite)
                .organisation(orgLogistique)
                .assignedTo(user1)
                .expectedCloseDate(LocalDate.now().plusDays(7))
                .notes("Visite technique confirmée pour la semaine prochaine.")
                .build();

        Deal deal4 = Deal.builder()
                .title("Partenariat TechnoPlus")
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepQualif)
                .organisation(orgTechno)
                .assignedTo(user2)
                .expectedCloseDate(LocalDate.now().plusDays(45))
                .build();

        Deal deal5 = Deal.builder()
                .title("Nettoyage médical — Centre du Parc")
                .amount(new BigDecimal("650.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepQualif)
                .organisation(orgSante)
                .assignedTo(user1)
                .expectedCloseDate(LocalDate.now().plusDays(60))
                .build();

        Deal deal6 = Deal.builder()
                .title("Prestation freelance — Nadia Osman")
                .amount(new BigDecimal("200.00"))
                .currency("EUR")
                .status(DealStatus.OPEN)
                .pipeline(pipeline)
                .pipelineStep(stepDevis)
                .assignedTo(user2)
                .expectedCloseDate(LocalDate.now().minusDays(3))
                .notes("Deal en retard — relance nécessaire.")
                .build();

        Deal deal7 = Deal.builder()
                .title("Contrat annuel TechnoPlus — Bureaux Liège")
                .amount(new BigDecimal("4200.00"))
                .currency("EUR")
                .status(DealStatus.WON)
                .pipeline(pipeline)
                .pipelineStep(stepWon)
                .organisation(orgTechno)
                .assignedTo(user2)
                .expectedCloseDate(LocalDate.now().minusDays(10))
                .closedAt(Instant.now().minusSeconds(86400 * 8))
                .notes("Contrat signé le 05/03. Démarrage le 01/04.")
                .build();

        Deal deal8 = Deal.builder()
                .title("Nettoyage chantier — Promoteur Inconnu")
                .amount(new BigDecimal("1800.00"))
                .currency("EUR")
                .status(DealStatus.LOST)
                .pipeline(pipeline)
                .pipelineStep(stepLost)
                .organisation(orgImmo)
                .assignedTo(user1)
                .expectedCloseDate(LocalDate.now().minusDays(20))
                .closedAt(Instant.now().minusSeconds(86400 * 18))
                .notes("Client a choisi un concurrent moins cher.")
                .build();

        dealRepository.saveAll(List.of(deal1, deal2, deal3, deal4, deal5, deal6, deal7, deal8));
        log.info("✅ Created 8 deals");

        // Create primary contact roles for each deal
        dealContactRoleRepository.saveAll(List.of(
            DealContactRole.builder().deal(deal1).contact(contactThomas).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal2).contact(contactMarc).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal3).contact(contactPierre).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal4).contact(contactSophie).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal5).contact(contactIsabelle).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal6).contact(contactNadia).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal7).contact(contactJean).role(ContactRole.DECISION_MAKER).primary(true).build(),
            DealContactRole.builder().deal(deal8).contact(contactMarc).role(ContactRole.DECISION_MAKER).primary(true).build()
        ));
        log.info("✅ Created 8 deal contact roles");

        log.info("🎉 CRM data initialization complete!");

        //endregion

            }


        // Méthode helper pour extraire la version majeure proprement
        private String extractMajorVersion(String osVersion) {
            if (osVersion == null || osVersion.isEmpty()) {
                return "Unknown";
            }
            try {
                return osVersion.split("\\.")[0];
            } catch (Exception e) {
                return osVersion; // Retourner la version complète si on ne peut pas extraire
            }
        }

}
