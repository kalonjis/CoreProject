package be.steby.CoreProject.dal.repositories.crm;

import be.steby.CoreProject.dl.entities.crm.CallLog;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CallLog} entity operations.
 *
 * <p>{@code CallLog} records are always accessed via their parent
 * {@link Interaction}. This repository
 * provides only the minimal queries needed for direct access and
 * call status reporting.</p>
 */
@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a call log by its public UUID.
     *
     * @param publicId the public UUID
     * @return the call log if found
     */
    Optional<CallLog> findByPublicId(String publicId);

    /**
     * Finds the call log associated with a specific interaction.
     *
     * <p>Used to load call details from the interaction detail view.</p>
     *
     * @param interactionId the internal ID of the parent interaction
     * @return the call log if present
     */
    Optional<CallLog> findByInteractionId(Long interactionId);

    // =========================================================================
    // Reporting
    // =========================================================================

    /**
     * Finds all call logs with a specific status for a given interaction's commercial.
     *
     * <p>Used to count unanswered calls and trigger automatic follow-up tasks.</p>
     *
     * @param status the call status to filter by (e.g. {@code NO_ANSWER})
     * @return call logs matching the status
     */
    List<CallLog> findByStatus(CallStatus status);
}