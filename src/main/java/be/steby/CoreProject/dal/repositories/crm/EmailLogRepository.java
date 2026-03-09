package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.EmailLog;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link EmailLog} entity operations.
 *
 * <p>{@code EmailLog} records are always accessed via their parent
 * {@link Interaction}. The most
 * critical lookup is by {@code externalMessageId}, used when processing
 * open and click webhook callbacks from the email provider.</p>
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds an email log by its public UUID.
     *
     * @param publicId the public UUID
     * @return the email log if found
     */
    Optional<EmailLog> findByPublicId(String publicId);

    /**
     * Finds the email log associated with a specific interaction.
     *
     * <p>Used to load email details from the interaction detail view.</p>
     *
     * @param interactionId the internal ID of the parent interaction
     * @return the email log if present
     */
    Optional<EmailLog> findByInteractionId(Long interactionId);

    /**
     * Finds an email log by the external provider message ID.
     *
     * <p>Used as the correlation key when processing webhook callbacks
     * from the email provider (open and click tracking events).
     * The service layer updates {@code openedAt} or {@code clickedAt}
     * on the found record.</p>
     *
     * @param externalMessageId the message ID assigned by the mail provider
     * @return the email log if found
     */
    Optional<EmailLog> findByExternalMessageId(String externalMessageId);
}