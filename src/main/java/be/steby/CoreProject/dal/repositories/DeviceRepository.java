package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
