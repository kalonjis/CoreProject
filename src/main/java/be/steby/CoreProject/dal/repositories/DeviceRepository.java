package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

}
