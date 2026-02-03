package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Lead;
import be.steby.CoreProject.dl.enums.LeadType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Lead} entity operations.
 *
 * <p>Provides data access methods for public inquiries from anonymous visitors,
 * including anti-spam rate limiting queries.</p>
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    /**
     * Finds an inquiry by its public ID.
     *
     * @param publicId the public UUID
     * @return the inquiry if found
     */
    Optional<Lead> findByPublicId(String publicId);

    /**
     * Counts inquiries from a specific IP address within a time window.
     *
     * <p>Used for rate limiting to prevent spam.</p>
     *
     * @param ipAddress the IP address to check
     * @param since     the start of the time window
     * @return number of inquiries from this IP
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
           "WHERE l.ipAddress = :ip AND l.submittedAt >= :since")
    long countByIpAddressSince(@Param("ip") String ipAddress, @Param("since") Instant since);

    /**
     * Counts inquiries from a specific email address within a time window.
     *
     * <p>Used for rate limiting to prevent spam.</p>
     *
     * @param email the email address to check
     * @param since the start of the time window
     * @return number of inquiries from this email
     */
    @Query("SELECT COUNT(l) FROM Lead l " +
           "WHERE LOWER(l.email) = LOWER(:email) AND l.submittedAt >= :since")
    long countByEmailSince(@Param("email") String email, @Param("since") Instant since);

    /**
     * Finds all inquiries by type.
     *
     * @param leadType the inquiry type
     * @return list of inquiries for this type
     */
    List<Lead> findByLeadTypeOrderBySubmittedAtDesc(LeadType leadType);

    /**
     * Finds all inquiries submitted within a time range.
     *
     * @param from start of the time range
     * @param to   end of the time range
     * @return list of inquiries in this range
     */
    @Query("SELECT l FROM Lead l " +
           "WHERE l.submittedAt >= :from AND l.submittedAt <= :to " +
           "ORDER BY l.submittedAt DESC")
    List<Lead> findBySubmittedAtBetween(
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}