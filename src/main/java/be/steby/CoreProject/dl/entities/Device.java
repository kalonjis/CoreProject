package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Device extends BaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String deviceType;

    @Column(length = 100)
    private String browser;

    @Column(length = 50)
    private String browserVersion;

    @Column(length = 100)
    private String operatingSystem;

    @Column(length = 50)
    private String osVersion;

    @Column(length = 50)
    private String device_cpu;


    @Column(length = 10)
    private String device_cpu_bits;

    @Column(length = 20)
    private String language;

    @Column(length = 100)
    private String deviceClass;

    @Column(length = 100)
    private String deviceBrand;

    @Column(length = 255, nullable = false, unique = true)
    private String fingerprint;

    @Column(nullable = false)
    private Instant firstSeen;

    @Column(nullable = false)
    private Instant lastSeen;

    @Column(length = 45)  // IPv6 peut faire jusqu'à 45 caractères
    private String lastIpAddress;

    private String location;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceTrustLevel deviceTrustLevel;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(nullable = false)
    private boolean blacklisted;

    private boolean firstDeviceUsed;


}