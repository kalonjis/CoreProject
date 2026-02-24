package be.steby.CoreProject.bll.domains.gdpr.services.data_collector;

import be.steby.CoreProject.bll.domains.gdpr.models.GdprUserDataSnapshot;
import be.steby.CoreProject.dl.entities.User;

/**
 * Collects all personal data for a given user and returns it
 * as a single immutable {@link GdprUserDataSnapshot}.
 *
 * <p>Called by {@link GdprExportService} during asynchronous archive generation.
 * Each domain (profile, devices, activity…) is queried independently
 * so that a failure in one domain does not block the others.
 */
public interface GdprDataCollectorService {

    /**
     * Aggregates all personal data for the given user.
     *
     * @param user the user whose data must be exported
     * @return a complete, immutable snapshot ready for serialization
     */
    GdprUserDataSnapshot collect(User user);
}