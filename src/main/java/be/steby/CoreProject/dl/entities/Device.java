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

    @Column(length = 50)
    private String device_cpu_bits;

    @Column(length = 50)
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

    @Column(length = 45)  // IPv6 can be up to 45 characters
    private String lastIpAddress;

    private String location;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceTrustLevel deviceTrustLevel;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(nullable = false)
    private boolean blacklisted;

    @Column
    private Instant blacklistedTime;

    @Column
    private boolean firstDeviceUsed;

    @Column(nullable = false)
    private boolean loggedOut = false;

    @Column
    private Instant logoutTime;

    /**
     * Generates a human-readable name for the device based on its properties.
     * This method was requested to be added for better device identification.
     *
     * @return A formatted device name string
     */
    public String getName() {
        StringBuilder name = new StringBuilder();

        // Start with browser information if available
        if (browser != null && !browser.isEmpty() && !"Unknown".equals(browser)) {
            name.append(browser);
            if (browserVersion != null && !browserVersion.isEmpty() && !"Unknown".equals(browserVersion)) {
                name.append(" ").append(browserVersion);
            }
        }

        // Add operating system information
        if (operatingSystem != null && !operatingSystem.isEmpty() && !"Unknown".equals(operatingSystem)) {
            if (name.length() > 0) {
                name.append(" on ");
            }
            name.append(operatingSystem);

            if (osVersion != null && !osVersion.isEmpty() && !"Unknown".equals(osVersion)) {
                name.append(" ").append(osVersion);
            }
        }

        // If we still don't have a good name, try device brand and class
        if (name.length() == 0) {
            if (deviceBrand != null && !deviceBrand.isEmpty() && !"Unknown".equals(deviceBrand)) {
                name.append(deviceBrand);

                if (deviceClass != null && !deviceClass.isEmpty() && !"Unknown".equals(deviceClass)) {
                    name.append(" ").append(deviceClass);
                }
            } else if (deviceClass != null && !deviceClass.isEmpty() && !"Unknown".equals(deviceClass)) {
                name.append(deviceClass);
            }
        }

        // Final fallback to device type
        if (name.length() == 0) {
            if (deviceType != null && !deviceType.isEmpty() && !"UNKNOWN".equals(deviceType)) {
                return deviceType.charAt(0) + deviceType.substring(1).toLowerCase() + " Device";
            } else {
                return "Unknown Device";
            }
        }

        return name.toString();
    }

    /**
     * Returns a short name suitable for UI display (max ~30 characters)
     */
    public String getShortName() {
        String fullName = getName();

        if (fullName.length() <= 30) {
            return fullName;
        }

        // Try to create a shorter version
        StringBuilder shortName = new StringBuilder();

        if (browser != null && !browser.isEmpty() && !"Unknown".equals(browser)) {
            shortName.append(browser);
        }

        if (operatingSystem != null && !operatingSystem.isEmpty() && !"Unknown".equals(operatingSystem)) {
            if (shortName.length() > 0) {
                shortName.append(" on ");
            }
            shortName.append(operatingSystem);
        }

        if (shortName.length() == 0) {
            return deviceType != null && !deviceType.equals("UNKNOWN") ?
                    deviceType.charAt(0) + deviceType.substring(1).toLowerCase() : "Device";
        }

        return shortName.length() <= 30 ? shortName.toString() : shortName.substring(0, 27) + "...";
    }

    /**
     * Checks if this device represents a mobile device
     */
    public boolean isMobile() {
        return "MOBILE".equals(deviceType) ||
                (deviceClass != null && deviceClass.toLowerCase().contains("phone"));
    }

    /**
     * Checks if this device represents a desktop/laptop
     */
    public boolean isDesktop() {
        return "DESKTOP".equals(deviceType) ||
                (deviceClass != null && deviceClass.toLowerCase().contains("desktop"));
    }

    /**
     * Checks if this device represents a tablet
     */
    public boolean isTablet() {
        return "TABLET".equals(deviceType) ||
                (deviceClass != null && deviceClass.toLowerCase().contains("tablet"));
    }
}