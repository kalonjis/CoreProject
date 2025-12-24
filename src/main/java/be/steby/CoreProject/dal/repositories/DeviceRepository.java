package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByFingerprint(String fingerprint);

    Optional<Device> findByIdAndUser(Long id, User user);

    List<Device> findAllByUser(User user);

    /**
     * Finds a Device by its public_id.
     * @param publicId the public_id from URL
     * @return Optional containing the device if found
     */
    @Query("SELECT d FROM Device d WHERE d.publicId = :publicId")
    Optional<Device> findByPublicId(String publicId);


    /**
     * Disconnects all devices for a user EXCEPT the specified device.
     * More performant than inferring user from device (avoids JOIN).
     *
     * @param user The user whose devices should be disconnected
     * @param currentDeviceId Device ID to keep connected
     * @return Number of devices disconnected
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Device d SET d.loggedOut = true " +
            "WHERE d.user = ?1 AND d.id != ?2 AND d.loggedOut = false")
    int disconnectAllDevicesExceptCurrent(User user, Long currentDeviceId);


    List<Device> findByUser(User user);


    /**
     * Counts devices by exact trust level.
     * Uses correct property name: deviceTrustLevel
     *
     * @param deviceTrustLevel Trust level to count
     * @return Number of devices with the specified trust level
     */
    Long countByDeviceTrustLevel(DeviceTrustLevel deviceTrustLevel);

    /**
     * Counts trusted devices (TRUSTED + HIGHLY_TRUSTED).
     * These are devices that have earned user's confidence.
     *
     * @return Number of trusted and highly trusted devices
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.deviceTrustLevel IN ('TRUSTED', 'HIGHLY_TRUSTED')")
    Long countTrustedDevices();

    /**
     * Counts untrusted devices (UNTRUSTED + BASIC).
     * These are new or unverified devices.
     *
     * @return Number of untrusted and basic trust level devices
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.deviceTrustLevel IN ('UNTRUSTED', 'BASIC')")
    Long countUntrustedDevices();


    /**
     * Counts devices that have been active since a specific date.
     * Active is defined as having lastSeen after the specified date.
     *
     * @param since Date threshold for activity
     * @return Number of active devices
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.lastSeen > :since")
    Long countActiveDevicesSince(@Param("since") Instant since);

    /**
     * Counts devices that have been active since a specific date.
     * Active is defined as having lastSeen after the specified date.
     *
     * @param since Date threshold for activity
     * @return Number of active devices
     */
    Long countByLastSeenAfter(Instant since);

}
